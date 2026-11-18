package de.citrinscore.score.scan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import de.citrinscore.score.render.PageSource
import de.citrinscore.score.render.luminanceOnWhite
import de.citrinscore.score.render.scanSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min

private const val SCAN_RESOLUTION = 2000f
private const val WINDOW_FRACTION = 1f / 16
private const val BLACK: Byte = 0
private const val WHITE: Byte = -1

const val DEFAULT_SCAN_THRESHOLD = 0.15f
val ScanThresholdRange = 0.02f..0.4f

class ScanPage(private val width: Int, private val height: Int, private val luminance: ByteArray) {

    val size get() = Size(width.toFloat(), height.toFloat())

    suspend fun binarized(threshold: Float, erased: List<EraserStroke> = emptyList()): ImageBitmap = withContext(Dispatchers.Default) {
        val ink = inkMask(threshold)
        val image = grayscaleImageBitmap(width, height, ByteArray(ink.size) { if (ink[it]) BLACK else WHITE })
        if (erased.isEmpty()) image else image.erased(erased)
    }

    suspend fun dewarped(): ScanPage = withContext(Dispatchers.Default) {
        StaffDewarping(width, height, inkMask(DEFAULT_SCAN_THRESHOLD)).apply(luminance)?.let { ScanPage(width, height, it) } ?: this@ScanPage
    }

    private fun inkMask(threshold: Float): BooleanArray {
        val stride = width + 1
        val integral = IntArray(stride * (height + 1))
        for (y in 0 until height) {
            var rowSum = 0
            for (x in 0 until width) {
                rowSum += luminance[y * width + x].toInt() and 0xFF
                integral[(y + 1) * stride + x + 1] = integral[y * stride + x + 1] + rowSum
            }
        }
        val radius = max(1, (width * WINDOW_FRACTION / 2).toInt())
        val factor = 1f - threshold
        val ink = BooleanArray(width * height)
        for (y in 0 until height) {
            val top = max(0, y - radius)
            val bottom = min(height, y + radius + 1)
            for (x in 0 until width) {
                val left = max(0, x - radius)
                val right = min(width, x + radius + 1)
                val sum = integral[bottom * stride + right] - integral[top * stride + right] -
                    integral[bottom * stride + left] + integral[top * stride + left]
                val count = (right - left) * (bottom - top)
                val value = luminance[y * width + x].toInt() and 0xFF
                ink[y * width + x] = value * count < sum * factor
            }
        }
        return ink
    }

    private fun ImageBitmap.erased(strokes: List<EraserStroke>) = ImageBitmap(width, height).also { target ->
        Canvas(target).apply {
            drawImage(this@erased, Offset.Zero, Paint())
            strokes.forEach { stroke -> drawPath(stroke.path(), eraserPaint(stroke.width)) }
        }
    }
}

private fun eraserPaint(width: Float) = Paint().apply {
    color = Color.White
    style = PaintingStyle.Stroke
    strokeWidth = width
    strokeCap = StrokeCap.Round
    strokeJoin = StrokeJoin.Round
}

suspend fun PageSource.scanPages(): List<ScanPage> = pageSizes.indices.map { scanPage(it) }

private suspend fun PageSource.scanPage(page: Int): ScanPage = withContext(Dispatchers.Default) {
    val size = pageSizes[page]
    val target = scanSize(size, SCAN_RESOLUTION / size.width)
    val pixels = IntArray(target.width * target.height).also { render(page, Rect(Offset.Zero, size), target).readPixels(it) }
    ScanPage(target.width, target.height, ByteArray(pixels.size) { pixels[it].luminanceOnWhite().toByte() }).dewarped()
}

internal expect fun grayscaleImageBitmap(width: Int, height: Int, luminance: ByteArray): ImageBitmap
