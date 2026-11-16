package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val ScanWidth = 1000
private const val InkLuminance = 160
private const val MinInkPixels = 3
private const val PaddingFraction = 0.01f

suspend fun contentCrop(file: PlatformFile): PageCrop = ImageSource(file).use { source ->
    val scale = min(1f, ScanWidth.toFloat() / source.size.width)
    val scanSize = IntSize(max(1, (source.size.width * scale).roundToInt()), max(1, (source.size.height * scale).roundToInt()))
    val scan = source.render(Rect(0f, 0f, source.size.width.toFloat(), source.size.height.toFloat()), scanSize)
    val bounds = scan.inkBounds() ?: return@use PageCrop(0, 0, source.size.width, source.size.height)
    val padding = source.size.width * PaddingFraction
    PageCrop(
        left = max(0, floor(bounds.left / scale - padding).toInt()),
        top = max(0, floor(bounds.top / scale - padding).toInt()),
        right = min(source.size.width, ceil(bounds.right / scale + padding).toInt()),
        bottom = min(source.size.height, ceil(bounds.bottom / scale + padding).toInt()),
    )
}

internal fun ImageBitmap.inkBounds(): Rect? {
    val pixels = IntArray(width * height).also { readPixels(it) }
    val rowInk = IntArray(height)
    val columnInk = IntArray(width)
    for (y in 0 until height) {
        for (x in 0 until width) {
            if (pixels[y * width + x].isInk()) {
                rowInk[y]++
                columnInk[x]++
            }
        }
    }
    val top = rowInk.indexOfFirst { it >= MinInkPixels }
    val left = columnInk.indexOfFirst { it >= MinInkPixels }
    if (top < 0 || left < 0) return null
    val bottom = rowInk.indexOfLast { it >= MinInkPixels } + 1
    val right = columnInk.indexOfLast { it >= MinInkPixels } + 1
    return Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
}

private fun Int.isInk(): Boolean {
    val alpha = ushr(24) and 0xFF
    val red = ushr(16) and 0xFF
    val green = ushr(8) and 0xFF
    val blue = and(0xFF)
    val luminance = (red * 299 + green * 587 + blue * 114) / 1000
    val onWhite = 255 - (255 - luminance) * alpha / 255
    return onWhite < InkLuminance
}
