package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.roundToIntRect

class BitmapPageSource(private val image: ImageBitmap) : PageSource {
    override val pageSizes = listOf(Size(image.width.toFloat(), image.height.toFloat()))

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap = ImageBitmap(size.width, size.height).also { target ->
        val source = region.roundToIntRect()
        Canvas(target).drawImageRect(
            image = image,
            srcOffset = source.topLeft,
            srcSize = source.size,
            dstOffset = IntOffset.Zero,
            dstSize = size,
            paint = Paint().apply { filterQuality = FilterQuality.Medium },
        )
    }

    override fun close() = Unit
}
