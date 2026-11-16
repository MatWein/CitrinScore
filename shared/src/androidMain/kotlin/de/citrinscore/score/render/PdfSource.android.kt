package de.citrinscore.score.render

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

actual suspend fun openPdfSource(file: PlatformFile): PageSource = withContext(Dispatchers.IO) {
    val descriptor = when (val androidFile = file.androidFile) {
        is AndroidFile.FileWrapper -> ParcelFileDescriptor.open(androidFile.file, ParcelFileDescriptor.MODE_READ_ONLY)
        is AndroidFile.UriWrapper -> FileKit.context.contentResolver.openFileDescriptor(androidFile.uri, "r")
            ?: throw IOException("Could not open ${androidFile.uri}")
    }
    AndroidPdfSource(PdfRenderer(descriptor))
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
