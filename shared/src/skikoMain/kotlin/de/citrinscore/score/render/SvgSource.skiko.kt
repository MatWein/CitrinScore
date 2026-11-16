package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.Data
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.svg.SVGDOM
import org.jetbrains.skia.svg.SVGLength
import org.jetbrains.skia.svg.SVGLengthContext
import org.jetbrains.skia.svg.SVGLengthUnit
import org.jetbrains.skia.Rect as SkiaRect

private const val CSS_DPI = 96f

actual suspend fun openSvgSource(file: PlatformFile): PageSource {
    val bytes = file.readBytes()
    return withContext(Dispatchers.Default) {
        SkiaSvgSource(Data.makeFromBytes(bytes).use(::SVGDOM))
    }
}

private class SkiaSvgSource(private val dom: SVGDOM) : PageSource {
    override val pageSizes: List<Size>

    init {
        val root = checkNotNull(dom.root) { "SVG has no root element" }
        val viewBox = root.viewBox ?: root.getIntrinsicSize(SVGLengthContext(0f, 0f, CSS_DPI)).let { SkiaRect.makeWH(it.x, it.y) }
        check(!viewBox.isEmpty) { "SVG has no size" }
        root.width = SVGLength(viewBox.width, SVGLengthUnit.PX)
        root.height = SVGLength(viewBox.height, SVGLengthUnit.PX)
        dom.setContainerSize(viewBox.width, viewBox.height)
        pageSizes = listOf(Size(viewBox.width, viewBox.height))
    }

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap {
        val bitmap = Bitmap().apply { allocN32Pixels(size.width, size.height) }
        Canvas(bitmap).use { canvas ->
            canvas.clear(Color.WHITE)
            canvas.scale(size.width / region.width, size.height / region.height)
            canvas.translate(-region.left, -region.top)
            dom.render(canvas)
        }
        return bitmap.setImmutable().asComposeImageBitmap()
    }

    override fun close() = dom.close()
}
