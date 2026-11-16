package de.citrinscore.score.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import de.citrinscore.files.openReadOnlyDescriptor
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun openPdfSource(file: PlatformFile): PageSource = withContext(Dispatchers.IO) {
    AndroidPdfSource(PdfRenderer(file.openReadOnlyDescriptor()))
}

private class AndroidPdfSource(private val renderer: PdfRenderer) : PageSource {
    override val pageSizes = (0 until renderer.pageCount).map { index ->
        renderer.openPage(index).use { Size(it.width.toFloat(), it.height.toFloat()) }
    }

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap = synchronized(renderer) {
        val bitmap = Bitmap.createBitmap(size.width, size.height, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        val transform = Matrix().apply {
            setTranslate(-region.left, -region.top)
            postScale(size.width / region.width, size.height / region.height)
        }
        renderer.openPage(page).use { it.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY) }
        bitmap.asImageBitmap()
    }

    override fun close() = renderer.close()
}
