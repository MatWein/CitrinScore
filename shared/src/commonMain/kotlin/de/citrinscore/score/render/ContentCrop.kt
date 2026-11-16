package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.PageCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val SCAN_WIDTH = 1000f
private const val INK_LUMINANCE = 160
private const val MIN_INK_PIXELS = 3
private const val PADDING_FRACTION = 0.01f

suspend fun PageSource.contentCrop(page: Int): PageCrop = withContext(Dispatchers.Default) {
    val size = pageSizes[page]
    val full = PageCrop(0f, 0f, size.width, size.height)
    val scale = SCAN_WIDTH / size.width
    val scanSize = IntSize(max(1, (size.width * scale).roundToInt()), max(1, (size.height * scale).roundToInt()))
    val bounds = render(page, full.rect, scanSize).inkBounds() ?: return@withContext full
    val padding = size.width * PADDING_FRACTION
    PageCrop(
        left = max(0f, floor(bounds.left / scale - padding)),
        top = max(0f, floor(bounds.top / scale - padding)),
        right = min(size.width, ceil(bounds.right / scale + padding)),
        bottom = min(size.height, ceil(bounds.bottom / scale + padding)),
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
    val top = rowInk.indexOfFirst { it >= MIN_INK_PIXELS }
    val left = columnInk.indexOfFirst { it >= MIN_INK_PIXELS }
    if (top < 0 || left < 0) return null
    val bottom = rowInk.indexOfLast { it >= MIN_INK_PIXELS } + 1
    val right = columnInk.indexOfLast { it >= MIN_INK_PIXELS } + 1
    return Rect(left.toFloat(), top.toFloat(), right.toFloat(), bottom.toFloat())
}

private fun Int.isInk(): Boolean {
    val alpha = ushr(24) and 0xFF
    val red = ushr(16) and 0xFF
    val green = ushr(8) and 0xFF
    val blue = and(0xFF)
    val luminance = (red * 299 + green * 587 + blue * 114) / 1000
    val onWhite = 255 - (255 - luminance) * alpha / 255
    return onWhite < INK_LUMINANCE
}
