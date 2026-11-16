package de.citrinscore.score.scan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
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

    suspend fun binarized(threshold: Float): ImageBitmap = withContext(Dispatchers.Default) {
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
        val output = ByteArray(width * height)
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
                output[y * width + x] = if (value * count < sum * factor) BLACK else WHITE
            }
        }
        grayscaleImageBitmap(width, height, output)
    }
}

suspend fun PageSource.scanPages(): List<ScanPage> = pageSizes.indices.map { scanPage(it) }

private suspend fun PageSource.scanPage(page: Int): ScanPage = withContext(Dispatchers.Default) {
    val size = pageSizes[page]
    val target = scanSize(size, SCAN_RESOLUTION / size.width)
    val pixels = IntArray(target.width * target.height).also { render(page, Rect(Offset.Zero, size), target).readPixels(it) }
    ScanPage(target.width, target.height, ByteArray(pixels.size) { pixels[it].luminanceOnWhite().toByte() })
}

internal expect fun grayscaleImageBitmap(width: Int, height: Int, luminance: ByteArray): ImageBitmap
