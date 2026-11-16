package de.citrinscore.score.render

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

internal const val SCAN_WIDTH = 1000f
private const val INK_LUMINANCE = 160
private const val MARK_LUMINANCE = 224
private const val MIN_INK_PIXELS = 3

internal fun scanSize(size: Size, scale: Float) =
    IntSize(max(1, (size.width * scale).roundToInt()), max(1, (size.height * scale).roundToInt()))

internal class InkScan(image: ImageBitmap) {
    private val width = image.width
    private val luminance = IntArray(width * image.height).apply {
        image.readPixels(this)
        indices.forEach { this[it] = this[it].luminanceOnWhite() }
    }

    val inkRows: List<Int>
    val inkColumns: List<Int>

    init {
        val rows = IntArray(image.height)
        val columns = IntArray(width)
        luminance.forEachIndexed { index, value ->
            if (value < INK_LUMINANCE) {
                rows[index / width]++
                columns[index % width]++
            }
        }
        inkRows = rows.indices.filter { rows[it] >= MIN_INK_PIXELS }
        inkColumns = columns.indices.filter { columns[it] >= MIN_INK_PIXELS }
    }

    fun isBridged(top: Int, bottom: Int) = (0 until width).any { x ->
        (top until bottom).all { y -> luminance[y * width + x] < MARK_LUMINANCE }
    }
}

internal fun Int.luminanceOnWhite(): Int {
    val alpha = ushr(24) and 0xFF
    val red = ushr(16) and 0xFF
    val green = ushr(8) and 0xFF
    val blue = and(0xFF)
    val luminance = (red * 299 + green * 587 + blue * 114) / 1000
    return 255 - (255 - luminance) * alpha / 255
}
