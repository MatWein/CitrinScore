package de.citrinscore.omr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private const val WINDOW = 320
private const val STEP = 320
private const val BATCH_SIZE = 8
private const val CLASSES = 6
private const val STEMS_RESTS = 1
private const val NOTEHEAD = 2
private const val CLEFS_KEYS = 3
private const val STAFF = 4
private const val SYMBOLS = 5
private const val NOISE_GRID_CELLS = 20
private const val MAX_FILTERED_SHARE = 0.5

internal class Predictions(
    val preprocessed: Mat,
    val notehead: Mat,
    val symbols: Mat,
    val staff: Mat,
    val clefsKeys: Mat,
    val stemsRest: Mat,
) : AutoCloseable {
    val masks get() = listOf(preprocessed, notehead, symbols, staff, clefsKeys, stemsRest)

    fun map(transform: (Mat) -> Mat) = Predictions(
        transform(preprocessed),
        transform(notehead),
        transform(symbols),
        transform(staff),
        transform(clefsKeys),
        transform(stemsRest),
    )

    override fun close() = masks.forEach(Mat::release)
}

internal class Segnet(private val environment: OrtEnvironment, modelPath: String) : AutoCloseable {
    private val session: OrtSession = environment.createSession(modelPath, OrtSession.SessionOptions())

    fun segment(preprocessed: Mat, onPatches: (Int, Int) -> Unit): Predictions {
        val height = preprocessed.rows()
        val width = preprocessed.cols()
        val gray = preprocessed.bytes()
        val rows = pythonRange(0, max(height, WINDOW), STEP)
        val columns = pythonRange(0, max(width, WINDOW), STEP)
        val total = rows.size * columns.size
        val patches = mutableListOf<IntArray>()
        val batch = mutableListOf<FloatArray>()
        fun flush() {
            patches += run(batch)
            batch.clear()
            onPatches(patches.size, total)
        }
        onPatches(0, total)
        for (yLoop in rows) {
            val y = min(yLoop, height - WINDOW)
            for (xLoop in columns) {
                val x = min(xLoop, width - WINDOW)
                batch += extractPatch(gray, width, height, x, y)
                if (batch.size == BATCH_SIZE) flush()
            }
        }
        if (batch.isNotEmpty()) flush()
        val merged = mergePatches(patches, width, height)
        fun mask(label: Int) = byteMat(height, width, ByteArray(merged.size) { if (merged[it] == label) 1 else 0 })
        return Predictions(preprocessed, mask(NOTEHEAD), mask(SYMBOLS), mask(STAFF), mask(CLEFS_KEYS), mask(STEMS_RESTS))
    }

    private fun extractPatch(gray: ByteArray, width: Int, height: Int, x: Int, y: Int): FloatArray {
        val patch = FloatArray(3 * WINDOW * WINDOW) { 255f }
        val y0 = max(y, 0)
        val x0 = max(x, 0)
        val y1 = min(y + WINDOW, height)
        val x1 = min(x + WINDOW, width)
        for (row in y0 until y1) {
            for (column in x0 until x1) {
                val value = (gray[row * width + column].toInt() and 0xFF).toFloat()
                val offset = (row - y0) * WINDOW + (column - x0)
                for (channel in 0 until 3) patch[channel * WINDOW * WINDOW + offset] = value
            }
        }
        return patch
    }

    private fun run(batch: List<FloatArray>): List<IntArray> {
        val input = FloatBuffer.allocate(batch.size * 3 * WINDOW * WINDOW).apply { batch.forEach { put(it) } }.rewind() as FloatBuffer
        val shape = longArrayOf(batch.size.toLong(), 3, WINDOW.toLong(), WINDOW.toLong())
        return OnnxTensor.createTensor(environment, input, shape).use { tensor ->
            session.run(mapOf("input" to tensor)).use { result ->
                val output = (result[0] as OnnxTensor).floatBuffer
                val pixels = WINDOW * WINDOW
                List(batch.size) { item ->
                    val base = item * CLASSES * pixels
                    IntArray(pixels) { pixel ->
                        var best = 0
                        var bestValue = output[base + pixel]
                        for (label in 1 until CLASSES) {
                            val value = output[base + label * pixels + pixel]
                            if (value > bestValue) {
                                best = label
                                bestValue = value
                            }
                        }
                        best
                    }
                }
            }
        }
    }

    private fun mergePatches(patches: List<IntArray>, width: Int, height: Int): IntArray {
        val sum = FloatArray(width * height)
        val weight = FloatArray(width * height)
        var index = 0
        for (iy in pythonRange(0, height, STEP)) {
            val y = min(iy, height - WINDOW)
            val y0 = max(y, 0)
            val y1 = min(y + WINDOW, height)
            for (ix in pythonRange(0, width, STEP)) {
                val x = min(ix, width - WINDOW)
                val x0 = max(x, 0)
                val x1 = min(x + WINDOW, width)
                val patch = patches[index++]
                for (row in 0 until y1 - y0) {
                    for (column in 0 until x1 - x0) {
                        val target = (y0 + row) * width + x0 + column
                        sum[target] += patch[row * WINDOW + column]
                        weight[target] += 1f
                    }
                }
            }
        }
        return IntArray(width * height) { (sum[it] / max(weight[it], 1f)).toInt() }
    }

    override fun close() = session.close()
}

internal fun filterNoise(predictions: Predictions): Predictions {
    val mask = noiseMask(predictions.staff) ?: return predictions
    val filtered = mask.use { maskMat -> predictions.map { source -> Mat().also { Core.bitwise_and(source, source, it, maskMat) } } }
    predictions.close()
    return filtered
}

private fun noiseMask(staff: Mat): Mat? {
    val height = staff.rows()
    val width = staff.cols()
    val cellHeight = height / NOISE_GRID_CELLS
    val cellWidth = width / NOISE_GRID_CELLS
    val gray = Mat().also { Core.multiply(staff, Scalar(255.0), it) }
    val rows = ceil(height.toDouble() / cellHeight).toInt()
    val columns = ceil(width.toDouble() / cellWidth).toInt()
    val grid = Array(rows) { i ->
        IntArray(columns) { j ->
            val y1 = i * cellHeight
            val x1 = j * cellWidth
            gray.region(x1, y1, min(x1 + cellWidth, width), min(y1 + cellHeight, height)).use { it.clone() }.use { estimateNoise(it).toInt() and 0xFF }
        }
    }
    gray.release()
    val mask = ByteArray(width * height)
    var filtered = 0
    for (i in 0 until rows) {
        for (j in 0 until columns) {
            val neighbors = listOfNotNull(
                grid.getOrNull(i - 1)?.get(j),
                grid[i].getOrNull(j - 1),
                grid.getOrNull(i + 1)?.get(j),
                grid[i].getOrNull(j + 1),
            )
            if (grid[i][j] > IMAGE_NOISE_LIMIT && neighbors.any { it > IMAGE_NOISE_LIMIT }) {
                filtered++
            } else {
                for (y in i * cellHeight until min((i + 1) * cellHeight, height)) {
                    for (x in j * cellWidth until min((j + 1) * cellWidth, width)) mask[y * width + x] = -1
                }
            }
        }
    }
    val total = rows * columns
    return if (filtered > 0 && filtered.toDouble() / total <= MAX_FILTERED_SHARE) byteMat(height, width, mask) else null
}

private fun estimateNoise(tile: Mat): Double {
    val kernel = Mat(3, 3, CvType.CV_64F).apply { put(0, 0, 1.0, -2.0, 1.0, -2.0, 4.0, -2.0, 1.0, -2.0, 1.0) }
    val filtered = kernel.use { Mat().also { result -> Imgproc.filter2D(tile, result, CvType.CV_64F, it) } }
    val values = DoubleArray(tile.rows() * tile.cols()).also { filtered.use { mat -> mat.get(0, 0, it) } }
    return values.sumOf { abs(it) } / (tile.rows() * tile.cols())
}
