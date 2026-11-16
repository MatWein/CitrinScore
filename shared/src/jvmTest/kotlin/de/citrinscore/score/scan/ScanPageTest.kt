package de.citrinscore.score.scan

import androidx.compose.ui.graphics.toAwtImage
import de.citrinscore.score.render.PdfSourceTest
import de.citrinscore.score.render.openPageSource
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.awt.GradientPaint
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ScanPageTest {

    private val root = Files.createTempDirectory("citrinscore-scan").toFile()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `ink stays black and shaded paper turns white`() = runTest {
        val photo = File(root, "photo.jpg")
        ImageIO.write(
            BufferedImage(1000, 1400, BufferedImage.TYPE_INT_RGB).apply {
                createGraphics().apply {
                    paint = GradientPaint(0f, 0f, Color(230, 225, 215), 1000f, 0f, Color(110, 105, 100))
                    fillRect(0, 0, 1000, 1400)
                    color = Color(40, 40, 40)
                    fillRect(100, 300, 800, 4)
                    fillRect(100, 700, 800, 4)
                    dispose()
                }
            },
            "jpg",
            photo,
        )

        val page = openPageSource(PlatformFile(photo)).use { it.scanPages() }.single()
        val image = page.binarized(DEFAULT_SCAN_THRESHOLD).toAwtImage()

        assertEquals(2000, image.width)
        assertEquals(Color.WHITE.rgb, image.getRGB(300, 1000))
        assertEquals(Color.WHITE.rgb, image.getRGB(1700, 1000))
        assertEquals(Color.BLACK.rgb, image.getRGB(300, 603))
        assertEquals(Color.BLACK.rgb, image.getRGB(1700, 1403))
    }

    @Test
    fun `every pdf page becomes a scan page`() = runTest {
        val pdf = PdfSourceTest.samplePdf(root)

        val pages = openPageSource(PlatformFile(pdf)).use { it.scanPages() }

        assertEquals(2, pages.size)
    }
}
