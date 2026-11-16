package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.PDPageContentStream
import org.apache.pdfbox.pdmodel.common.PDRectangle
import java.awt.Color
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class PdfSourceTest {

    private val root = Files.createTempDirectory("citrinscore-pdf").toFile()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `pages report their size in points`() = runTest {
        val pdf = samplePdf(root)

        openPdfSource(PlatformFile(pdf)).use { source ->
            assertEquals(listOf(Size(600f, 800f), Size(800f, 600f)), source.pageSizes)
        }
    }

    @Test
    fun `white margins of pdf pages are cropped`() = runTest {
        val pdf = samplePdf(root)

        openPdfSource(PlatformFile(pdf)).use { source ->
            assertEquals(PageCrop(93f, 193f, 506f, 506f), source.contentCrop(0))
        }
    }

    @Test
    fun `a region renders at the requested pixel size`() = runTest {
        val pdf = samplePdf(root)

        val image = openPdfSource(PlatformFile(pdf)).use { it.render(0, Rect(0f, 100f, 600f, 400f), IntSize(1200, 600)) }.toAwtImage()

        assertEquals(1200, image.width)
        assertEquals(600, image.height)
        assertEquals(Color.WHITE.rgb, image.getRGB(100, 300))
        assertEquals(Color.BLACK.rgb, image.getRGB(600, 300))
        assertEquals(Color.WHITE.rgb, image.getRGB(600, 100))
    }

    companion object {
        fun samplePdf(directory: File): File = File(directory, "sample.pdf").also { file ->
            PDDocument().use { document ->
                listOf(PDRectangle(600f, 800f), PDRectangle(800f, 600f)).forEach { box ->
                    val page = PDPage(box)
                    document.addPage(page)
                    PDPageContentStream(document, page).use { content ->
                        content.setNonStrokingColor(Color.BLACK)
                        content.addRect(100f, box.height - 500f, 400f, 300f)
                        content.fill()
                    }
                }
                document.save(file)
            }
        }
    }
}
