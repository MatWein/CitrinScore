package de.citrinscore.score.render

import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ContentCropTest {

    private val root = Files.createTempDirectory("citrinscore-crop").toFile()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `white margins and specks are cropped with a small padding`() = runTest {
        val file = image(2000, 2800) { fillRect(400, 600, 1200, 1600); fillRect(100, 100, 3, 3) }

        assertEquals(PageCrop(380f, 580f, 1620f, 2220f), crop(file))
    }

    @Test
    fun `light gray paper counts as white`() = runTest {
        val file = image(1000, 1000, paper = Color(215, 215, 215)) { fillRect(300, 300, 400, 400) }

        assertEquals(PageCrop(290f, 290f, 710f, 710f), crop(file))
    }

    @Test
    fun `an empty page keeps its full size`() = runTest {
        val file = image(800, 600) {}

        assertEquals(PageCrop(0f, 0f, 800f, 600f), crop(file))
    }

    private suspend fun crop(file: File) = openImageSource(PlatformFile(file)).use { it.contentCrop(0) }

    private fun image(width: Int, height: Int, paper: Color = Color.WHITE, ink: java.awt.Graphics2D.() -> Unit): File {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            color = paper
            fillRect(0, 0, width, height)
            color = Color.BLACK
            ink()
            dispose()
        }
        return File(root, "page.png").also { ImageIO.write(image, "png", it) }
    }
}
