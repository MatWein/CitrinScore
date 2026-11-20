package de.citrinscore.score.scan

import androidx.compose.ui.geometry.Offset
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
import kotlin.test.assertTrue
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

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
    fun `curved staff lines are straightened`() = runTest {
        val photo = File(root, "curved.png")
        val staffTops = listOf(200, 600, 1000)
        ImageIO.write(
            BufferedImage(1000, 1400, BufferedImage.TYPE_INT_RGB).apply {
                createGraphics().apply {
                    color = Color.WHITE
                    fillRect(0, 0, 1000, 1400)
                    color = Color.BLACK
                    for (x in 0 until 1000) {
                        val bend = (12 * sin(PI * x / 1000) + 0.01 * x).roundToInt()
                        staffTops.forEach { top -> repeat(5) { line -> fillRect(x, top + line * 10 + bend, 1, 2) } }
                    }
                    dispose()
                }
            },
            "png",
            photo,
        )

        val image = openPageSource(PlatformFile(photo)).use { it.scanPages() }.single().binarized(DEFAULT_SCAN_THRESHOLD).toAwtImage()

        staffTops.forEach { top ->
            val heights = (100 until 1900 step 200).map { x -> (top * 2 - 40 until top * 2 + 140).first { image.getRGB(x, it) == Color.BLACK.rgb } }
            assertTrue(heights.max() - heights.min() <= 2, "staff at $top has line heights $heights")
        }
    }

    @Test
    fun `staff lines interrupted on a tilted page are straightened without a step`() = runTest {
        val photo = File(root, "interrupted.png")
        val staffTops = listOf(200, 600, 1000)
        ImageIO.write(
            BufferedImage(1000, 1400, BufferedImage.TYPE_INT_RGB).apply {
                createGraphics().apply {
                    color = Color.WHITE
                    fillRect(0, 0, 1000, 1400)
                    color = Color.BLACK
                    for (x in 0 until 1000) {
                        val bend = (0.04 * x).roundToInt()
                        staffTops.forEach { top ->
                            if (top == staffTops.last() && x in 400 until 600) return@forEach
                            repeat(5) { line -> fillRect(x, top + line * 10 + bend, 1, 2) }
                        }
                    }
                    dispose()
                }
            },
            "png",
            photo,
        )

        val image = openPageSource(PlatformFile(photo)).use { it.scanPages() }.single().binarized(DEFAULT_SCAN_THRESHOLD).toAwtImage()

        staffTops.forEach { top ->
            val heights = listOf(100, 300, 500, 700, 1300, 1500, 1700, 1900).map { x -> (top * 2 - 100 until top * 2 + 200).first { image.getRGB(x, it) == Color.BLACK.rgb } }
            assertTrue(heights.max() - heights.min() <= 3, "staff at $top has line heights $heights")
        }
    }

    @Test
    fun `erased strokes turn ink white`() = runTest {
        val scan = File(root, "ink.png")
        ImageIO.write(
            BufferedImage(1000, 1400, BufferedImage.TYPE_INT_RGB).apply {
                createGraphics().apply {
                    color = Color.WHITE
                    fillRect(0, 0, 1000, 1400)
                    color = Color.BLACK
                    fillRect(100, 300, 800, 20)
                    dispose()
                }
            },
            "png",
            scan,
        )
        val page = openPageSource(PlatformFile(scan)).use { it.scanPages() }.single()

        val image = page.binarized(DEFAULT_SCAN_THRESHOLD, listOf(EraserStroke(listOf(Offset(300f, 620f), Offset(700f, 620f)), 60f))).toAwtImage()

        assertEquals(Color.WHITE.rgb, image.getRGB(500, 620))
        assertEquals(Color.BLACK.rgb, image.getRGB(1500, 620))
    }

    @Test
    fun `every pdf page becomes a scan page`() = runTest {
        val pdf = PdfSourceTest.samplePdf(root)

        val pages = openPageSource(PlatformFile(pdf)).use { it.scanPages() }

        assertEquals(2, pages.size)
    }
}
