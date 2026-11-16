package de.citrinscore.score.render

import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SystemBreaksTest {

    private val root = Files.createTempDirectory("citrinscore-breaks").toFile()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `white gaps between systems become breaks`() = runTest {
        val file = image { system(top = 100); system(top = 500) }

        assertEquals(listOf(450f), breaks(file))
    }

    @Test
    fun `staves joined by a faint system line stay together`() = runTest {
        val file = image { system(top = 100, line = Color(200, 200, 200)) }

        assertEquals(emptyList(), breaks(file))
    }

    @Test
    fun `small gaps inside a system are ignored`() = runTest {
        val file = image { fillRect(100, 100, 800, 50); fillRect(100, 155, 800, 50) }

        assertEquals(emptyList(), breaks(file))
    }

    private suspend fun breaks(file: File) =
        SourceScoreRenderer(listOf(SourcePage(PlatformFile(file), 0, PageCrop(0f, 0f, 1000f, 1000f))), ::openImageSource)
            .use { it.systemBreaks(0) }

    private fun Graphics2D.system(top: Int, line: Color = Color.BLACK) {
        fillRect(100, top, 800, 100)
        fillRect(100, top + 200, 800, 100)
        color = line
        fillRect(100, top, 1, 300)
        color = Color.BLACK
    }

    private fun image(ink: Graphics2D.() -> Unit): File {
        val image = BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            color = Color.WHITE
            fillRect(0, 0, 1000, 1000)
            color = Color.BLACK
            ink()
            dispose()
        }
        return File(root, "page.png").also { ImageIO.write(image, "png", it) }
    }
}
