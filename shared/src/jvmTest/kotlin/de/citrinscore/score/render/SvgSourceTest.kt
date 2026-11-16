package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SvgSourceTest {

    private val root = Files.createTempDirectory("citrinscore-svg").toFile()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `the view box defines the page size`() = runTest {
        val svg = svgFile("""width="50mm" height="25mm" viewBox="10 20 200 100"""")

        openImageSource(PlatformFile(svg)).use { source ->
            assertEquals(listOf(Size(200f, 100f)), source.pageSizes)
        }
    }

    @Test
    fun `the document size is used without a view box`() = runTest {
        val svg = svgFile("""width="300" height="150"""")

        openImageSource(PlatformFile(svg)).use { source ->
            assertEquals(listOf(Size(300f, 150f)), source.pageSizes)
        }
    }

    @Test
    fun `a region renders at the requested pixel size`() = runTest {
        val svg = svgFile("""width="50mm" height="25mm" viewBox="10 20 200 100"""")

        val image = openImageSource(PlatformFile(svg)).use { it.render(0, Rect(40f, 0f, 140f, 100f), IntSize(400, 400)) }.toAwtImage()

        assertEquals(400, image.width)
        assertEquals(400, image.height)
        assertEquals(Color.WHITE.rgb, image.getRGB(20, 200))
        assertEquals(Color.BLACK.rgb, image.getRGB(200, 200))
        assertEquals(Color.WHITE.rgb, image.getRGB(200, 50))
    }

    private fun svgFile(attributes: String) = File(root, "page.svg").apply {
        writeText(
            """
            <svg xmlns="http://www.w3.org/2000/svg" $attributes>
              <rect x="60" y="45" width="50" height="50" fill="black"/>
            </svg>
            """.trimIndent(),
        )
    }
}
