package de.citrinscore.omr

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import java.nio.FloatBuffer
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min

private const val SEPARATE = 0
private const val GRAND_STAFF = 2
private const val SAME_STAFF = 1
private const val CERTAIN_PROBABILITY = 0.9
private const val PIXELS_PER_UNIT = 4
private const val MARGIN_UNITS = 10
private const val START_UNITS = 14
private const val STAFF_ROWS = 16
private const val OUTER_ROWS = 12
private const val GAP_ROWS = 40
private const val CROP_HEIGHT = 2 * (OUTER_ROWS + STAFF_ROWS) + GAP_ROWS
private const val CROP_WIDTH = (MARGIN_UNITS + START_UNITS) * PIXELS_PER_UNIT
private const val CHANNELS = 5
private const val END_UNITS = 16
private const val HORIZONTAL_END_WIDTH = END_UNITS * PIXELS_PER_UNIT
private const val HORIZONTAL_GAP_WIDTH = 64
private const val HORIZONTAL_CROP_WIDTH = 2 * HORIZONTAL_END_WIDTH + HORIZONTAL_GAP_WIDTH
private const val HORIZONTAL_CROP_HEIGHT = 2 * OUTER_ROWS + STAFF_ROWS
private const val HORIZONTAL_FEATURES = 5
private const val EXTRAPOLATION_UNITS = 20
private const val ROW_OFFSET_UNITS = 4
private const val OUTER_UNITS = 3

internal class StaffRelations(private val environment: OrtEnvironment, relationsModel: String, piecesModel: String) : AutoCloseable {
    private val relations = environment.createSession(relationsModel, OrtSession.SessionOptions())
    private val pieces = environment.createSession(piecesModel, OrtSession.SessionOptions())

    fun groupStaffs(predictions: Predictions, staffs: List<Staff>): List<MultiStaff> {
        val joined = joinPieces(predictions, staffs)
        val pairs = verticalPairs(joined)
        if (pairs.isEmpty()) return groupStaffs(joined, pairs, emptyList())
        val crops = PageChannels(predictions, joined.map { it.averageUnitSize }.median()).use { page ->
            pairs.map { (i, j) -> page.pairCrop(joined[i], joined[j]) }
        }
        return groupStaffs(joined, pairs, predict(relations, crops, CROP_HEIGHT, CROP_WIDTH, null))
    }

    private fun joinPieces(predictions: Predictions, staffs: List<Staff>): List<Staff> {
        val pairs = horizontalPairs(staffs)
        if (pairs.isEmpty()) return staffs
        val crops = PageChannels(predictions, staffs.map { it.averageUnitSize }.median()).use { page ->
            pairs.map { (i, j) -> page.horizontalCrop(staffs[i], staffs[j]) }
        }
        val features = pairs.map { (i, j) -> horizontalFeatures(staffs[i], staffs[j]) }
        val probabilities = predict(pieces, crops, HORIZONTAL_CROP_HEIGHT, HORIZONTAL_CROP_WIDTH, features)
        val rightOf = pairs.zip(probabilities).filter { (_, p) -> p[SAME_STAFF] > 0.5 }.associate { (pair, _) -> pair }
        val used = mutableSetOf<Int>()
        val result = mutableListOf<Staff>()
        for (i in staffs.indices.sortedBy { staffs[it].minX }) {
            if (!used.add(i)) continue
            var staff = staffs[i]
            var j = i
            while (rightOf[j] != null && rightOf.getValue(j) !in used) {
                j = rightOf.getValue(j)
                used += j
                staff = Staff(staff.grid + staffs[j].grid.filter { it.x > staff.maxX })
            }
            result += staff
        }
        return result.sortedBy { it.minY }
    }

    private fun predict(session: OrtSession, crops: List<FloatArray>, height: Int, width: Int, features: List<FloatArray>?): List<DoubleArray> {
        val size = CHANNELS * height * width
        val input = FloatBuffer.allocate(crops.size * size).apply { crops.forEach { put(it) } }.rewind() as FloatBuffer
        val inputs = mutableMapOf("crop" to OnnxTensor.createTensor(environment, input, longArrayOf(crops.size.toLong(), CHANNELS.toLong(), height.toLong(), width.toLong())))
        if (features != null) {
            val values = FloatBuffer.allocate(features.size * HORIZONTAL_FEATURES).apply { features.forEach { put(it) } }.rewind() as FloatBuffer
            inputs["features"] = OnnxTensor.createTensor(environment, values, longArrayOf(features.size.toLong(), HORIZONTAL_FEATURES.toLong()))
        }
        try {
            return session.run(inputs).use { result ->
                @Suppress("UNCHECKED_CAST")
                val logits = result[0].value as Array<FloatArray>
                logits.map { row ->
                    val maximum = row.max()
                    val exps = row.map { exp((it - maximum).toDouble()) }
                    val sum = exps.sum()
                    exps.map { it / sum }.toDoubleArray()
                }
            }
        } finally {
            inputs.values.forEach(OnnxTensor::close)
        }
    }

    override fun close() {
        relations.close()
        pieces.close()
    }
}

private class PageChannels(predictions: Predictions, unitSize: Double) : AutoCloseable {
    val scale = min(1.0, PIXELS_PER_UNIT / unitSize)
    private val layers: List<Mat>

    init {
        val gray = predictions.preprocessed
        val size = Size(max(1, roundHalfEven(gray.cols() * scale)).toDouble(), max(1, roundHalfEven(gray.rows() * scale)).toDouble())
        val sources = listOf(
            Mat().also { Core.bitwise_not(gray, it) },
            scaled(predictions.staff),
            scaled(predictions.symbols),
            scaled(predictions.stemsRest),
            scaled(predictions.clefsKeys),
        )
        layers = sources.map { source ->
            source.use { Mat().also { resized -> Imgproc.resize(it, resized, size, 0.0, 0.0, Imgproc.INTER_AREA) } }
                .use { resized -> Mat().also { resized.convertTo(it, CvType.CV_32F, 1.0 / 255) } }
        }
    }

    private fun scaled(mask: Mat) = Mat().also { Core.multiply(mask, Scalar(255.0), it) }

    fun pairCrop(upper: Staff, lower: Staff): FloatArray {
        val unit = (upper.averageUnitSize + lower.averageUnitSize) / 2
        val left = min(upper.minX, lower.minX)
        val x = DoubleArray(CROP_WIDTH) { left + (it.toDouble() / PIXELS_PER_UNIT - MARGIN_UNITS) * unit }
        val anchorRows = listOf(0, OUTER_ROWS, OUTER_ROWS + STAFF_ROWS, OUTER_ROWS + STAFF_ROWS + GAP_ROWS, OUTER_ROWS + 2 * STAFF_ROWS + GAP_ROWS, CROP_HEIGHT)
        val anchorYs = x.map { column ->
            val upperTop = upper.lineAt(0, column)
            val upperBottom = upper.lineAt(-1, column)
            val lowerTop = lower.lineAt(0, column)
            val lowerBottom = lower.lineAt(-1, column)
            listOf(upperTop - OUTER_UNITS * unit, upperTop, upperBottom, lowerTop, lowerBottom, lowerBottom + OUTER_UNITS * unit)
        }
        return remap(x, rows(anchorRows, anchorYs, CROP_HEIGHT))
    }

    fun horizontalCrop(left: Staff, right: Staff): FloatArray {
        val unit = (left.averageUnitSize + right.averageUnitSize) / 2
        val share = DoubleArray(HORIZONTAL_END_WIDTH) { it.toDouble() / HORIZONTAL_END_WIDTH }
        val gap = linspace(left.maxX, right.minX, HORIZONTAL_GAP_WIDTH)
        val x = share.map { left.maxX + (it - 1) * END_UNITS * unit } + gap + share.map { right.minX + it * END_UNITS * unit }
        val blend = DoubleArray(HORIZONTAL_END_WIDTH).asList() + linspace(0.0, 1.0, HORIZONTAL_GAP_WIDTH) + List(HORIZONTAL_END_WIDTH) { 1.0 }
        val anchorRows = listOf(0, OUTER_ROWS, OUTER_ROWS + STAFF_ROWS, HORIZONTAL_CROP_HEIGHT)
        val anchorYs = x.mapIndexed { column, value ->
            val b = blend[column]
            val top = (1 - b) * left.lineAt(0, value) + b * right.lineAt(0, value)
            val bottom = (1 - b) * left.lineAt(-1, value) + b * right.lineAt(-1, value)
            listOf(top - OUTER_UNITS * unit, top, bottom, bottom + OUTER_UNITS * unit)
        }
        return remap(x.toDoubleArray(), rows(anchorRows, anchorYs, HORIZONTAL_CROP_HEIGHT))
    }

    private fun rows(anchorRows: List<Int>, anchorYs: List<List<Double>>, height: Int): Array<DoubleArray> = Array(height) { row ->
        val segment = (0 until anchorRows.size - 1).first { row < anchorRows[it + 1] }
        val start = anchorRows[segment]
        val end = anchorRows[segment + 1]
        val part = (row - start).toDouble() / (end - start)
        DoubleArray(anchorYs.size) { column -> anchorYs[column][segment] + part * (anchorYs[column][segment + 1] - anchorYs[column][segment]) }
    }

    private fun remap(x: DoubleArray, rows: Array<DoubleArray>): FloatArray {
        val height = rows.size
        val width = x.size
        val mapX = floatMat(height, width, FloatArray(height * width) { (x[it % width] * scale).toFloat() })
        val mapY = floatMat(height, width, FloatArray(height * width) { (rows[it / width][it % width] * scale).toFloat() })
        val result = FloatArray(CHANNELS * height * width)
        layers.forEachIndexed { channel, layer ->
            Mat().use { remapped ->
                Imgproc.remap(layer, remapped, mapX, mapY, Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(0.0))
                remapped.floats().copyInto(result, channel * height * width)
            }
        }
        mapX.release()
        mapY.release()
        return result
    }

    override fun close() = layers.forEach(Mat::release)
}

private fun linspace(start: Double, stop: Double, count: Int): List<Double> {
    val step = (stop - start) / (count - 1)
    return List(count) { if (it == count - 1) stop else start + it * step }
}

private fun overlapsHorizontally(upper: Staff, lower: Staff): Boolean {
    val overlap = min(upper.maxX, lower.maxX) - max(upper.minX, lower.minX)
    return overlap > 0.3 * min(upper.maxX - upper.minX, lower.maxX - lower.minX)
}

private fun verticalPairs(staffs: List<Staff>): List<Pair<Int, Int>> {
    val order = staffs.indices.sortedBy { staffs[it].minY }
    return order.mapIndexedNotNull { position, i ->
        order.drop(position + 1).filter { j -> overlapsHorizontally(staffs[i], staffs[j]) && staffs[j].minY > staffs[i].minY }
            .minByOrNull { staffs[it].minY }?.let { i to it }
    }
}

private fun centerAt(staff: Staff, x: Double): Double {
    val xs = DoubleArray(staff.grid.size) { staff.grid[it].x }
    val ys = DoubleArray(staff.grid.size) { (staff.grid[it].y.first() + staff.grid[it].y.last()) / 2 }
    if ((xs.first() <= x && x <= xs.last()) || xs.size < 2) return interpolate(x, xs, ys)
    val reach = EXTRAPOLATION_UNITS * staff.averageUnitSize
    var near = xs.indices.filter { if (x > xs.last()) xs[it] >= xs.last() - reach else xs[it] <= xs.first() + reach }
    if (near.size < 2) near = if (x > xs.last()) listOf(xs.size - 2, xs.size - 1) else listOf(0, 1)
    val meanX = near.map { xs[it] }.average()
    val meanY = near.map { ys[it] }.average()
    val slope = near.sumOf { (xs[it] - meanX) * (ys[it] - meanY) } / near.sumOf { (xs[it] - meanX) * (xs[it] - meanX) }
    return slope * x + (meanY - slope * meanX)
}

private fun offset(left: Staff, right: Staff): Double {
    val middle = (left.maxX + right.minX) / 2
    return centerAt(right, middle) - centerAt(left, middle)
}

private fun isSameRow(left: Staff, right: Staff): Boolean {
    if (right.minX <= left.minX || right.maxX <= left.maxX) return false
    val unit = (left.averageUnitSize + right.averageUnitSize) / 2
    return abs(offset(left, right)) < ROW_OFFSET_UNITS * unit
}

private fun horizontalPairs(staffs: List<Staff>): List<Pair<Int, Int>> = staffs.mapIndexedNotNull { i, left ->
    staffs.indices.filter { j -> j != i && isSameRow(left, staffs[j]) }.minByOrNull { staffs[it].minX }?.let { i to it }
}

private fun horizontalFeatures(left: Staff, right: Staff): FloatArray {
    val unit = (left.averageUnitSize + right.averageUnitSize) / 2
    return floatArrayOf(
        (offset(left, right) / unit).toFloat(),
        ((right.minX - left.maxX) / unit / 10).toFloat(),
        ln(left.averageUnitSize / right.averageUnitSize).toFloat(),
        ((left.maxX - left.minX) / unit / 100).toFloat(),
        ((right.maxX - right.minX) / unit / 100).toFloat(),
    )
}

private fun selectGrandStaffs(pairs: List<Pair<Int, Int>>, probabilities: List<DoubleArray>): List<Int> {
    val candidates = probabilities.indices.filter { probabilities[it][GRAND_STAFF] == probabilities[it].max() && probabilities[it][SEPARATE] < 0.5 }
    val used = mutableSetOf<Int>()
    return candidates.sortedBy { -probabilities[it][GRAND_STAFF] }.filter { k ->
        val (upper, lower) = pairs[k]
        if (upper in used || lower in used) return@filter false
        used += upper
        used += lower
        true
    }
}

private fun mergeGrandStaff(upper: Staff, lower: Staff): Staff {
    val common = upper.grid.map { roundHalfEven(it.x) }.toSet() intersect lower.grid.map { roundHalfEven(it.x) }.toSet()
    if (common.size >= 2) return upper.merge(lower)
    val points = upper.grid.filter { lower.minX <= it.x && it.x <= lower.maxX }
    if (points.isEmpty()) return upper
    val lines = lower.grid.first().y.size
    val resampled = Staff(points.map { point -> StaffPoint(point.x, List(lines) { lower.lineAt(it, point.x) }, point.angle) })
    return Staff(points).merge(resampled)
}

private fun groupStaffs(staffs: List<Staff>, pairs: List<Pair<Int, Int>>, probabilities: List<DoubleArray>): List<MultiStaff> {
    val systemOf = IntArray(staffs.size) { it }
    fun find(index: Int): Int {
        var i = index
        while (systemOf[i] != i) i = systemOf[i]
        return i
    }
    pairs.zip(probabilities).forEach { (pair, p) -> if (p[SEPARATE] < 0.5) systemOf[find(pair.second)] = find(pair.first) }
    val grandStaffs = selectGrandStaffs(pairs, probabilities).associate { pairs[it] }
    val lowerStaffs = grandStaffs.values.toSet()
    val systems = LinkedHashMap<Int, MutableList<Staff>>()
    staffs.forEachIndexed { i, staff ->
        if (i in lowerStaffs) return@forEachIndexed
        val merged = grandStaffs[i]?.let { mergeGrandStaff(staff, staffs[it]) } ?: staff
        systems.getOrPut(find(i)) { mutableListOf() } += merged
    }
    val uncertain = pairs.zip(probabilities).filter { (pair, p) -> find(pair.first) == find(pair.second) && p[SEPARATE] > 1 - CERTAIN_PROBABILITY }
        .map { find(it.first.first) }.toSet()
    return systems.map { (root, system) -> MultiStaff(system, certain = system.size > 1 && root !in uncertain) }.sortedBy { it.staffs.first().minY }
}
