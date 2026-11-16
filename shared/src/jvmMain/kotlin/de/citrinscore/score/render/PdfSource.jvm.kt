package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.Color
import java.awt.image.BufferedImage

actual suspend fun openPdfSource(file: PlatformFile): PageSource {
    val bytes = file.readBytes()
    return withContext(Dispatchers.IO) { PdfBoxSource(Loader.loadPDF(bytes)) }
}

private class PdfBoxSource(private val document: PDDocument) : PageSource {
    private val renderer = PDFRenderer(document)

    override val pageSizes = document.pages.map { page -> orientedPageSize(page.cropBox.width, page.cropBox.height, page.rotation) }

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap = synchronized(document) {
        val image = BufferedImage(size.width, size.height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        try {
            graphics.background = Color.WHITE
            graphics.clearRect(0, 0, size.width, size.height)
            val scaleX = size.width / region.width
            val scaleY = size.height / region.height
            graphics.translate(-region.left * scaleX.toDouble(), -region.top * scaleY.toDouble())
            renderer.renderPageToGraphics(page, graphics, scaleX, scaleY)
        } finally {
            graphics.dispose()
        }
        image.toComposeImageBitmap()
    }

    override fun close() = document.close()
}
