package de.citrinscore.score.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.RectF
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import com.caverock.androidsvg.PreserveAspectRatio
import com.caverock.androidsvg.RenderOptions
import com.caverock.androidsvg.SVG
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun openSvgSource(file: PlatformFile): PageSource {
    val bytes = file.readBytes()
    return withContext(Dispatchers.Default) { AndroidSvgSource(SVG.getFromInputStream(bytes.inputStream())) }
}

private class AndroidSvgSource(private val svg: SVG) : PageSource {
    private val viewBox = svg.documentViewBox ?: RectF(0f, 0f, svg.documentWidth, svg.documentHeight)

    init {
        check(viewBox.width() > 0f && viewBox.height() > 0f) { "SVG has no size" }
    }

    override val pageSizes = listOf(Size(viewBox.width(), viewBox.height()))

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap {
        val output = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.WHITE) }
        val options = RenderOptions()
            .viewBox(viewBox.left + region.left, viewBox.top + region.top, region.width, region.height)
            .viewPort(0f, 0f, size.width.toFloat(), size.height.toFloat())
            .preserveAspectRatio(PreserveAspectRatio.STRETCH)
        svg.renderToCanvas(Canvas(output), options)
        return output.asImageBitmap()
    }

    override fun close() = Unit
}
