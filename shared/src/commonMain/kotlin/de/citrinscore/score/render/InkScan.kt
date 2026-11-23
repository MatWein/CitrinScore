package de.citrinscore.score.render

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import kotlin.math.max
import kotlin.math.roundToInt

internal const val SCAN_WIDTH = 1000f
private const val INK_LUMINANCE = 160
private const val MIN_INK_PIXELS = 3
private const val MAX_CHANNEL = 255
private const val RED_WEIGHT = 299
private const val GREEN_WEIGHT = 587
private const val BLUE_WEIGHT = 114
private const val WEIGHT_TOTAL = RED_WEIGHT + GREEN_WEIGHT + BLUE_WEIGHT

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
}

internal fun Int.luminanceOnWhite(): Int {
    val alpha = ushr(24) and 0xFF
    val red = ushr(16) and 0xFF
    val green = ushr(8) and 0xFF
    val blue = and(0xFF)
    val luminance = (red * RED_WEIGHT + green * GREEN_WEIGHT + blue * BLUE_WEIGHT) / WEIGHT_TOTAL
    return MAX_CHANNEL - (MAX_CHANNEL - luminance) * alpha / MAX_CHANNEL
}
