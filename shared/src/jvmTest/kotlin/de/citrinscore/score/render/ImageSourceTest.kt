package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class ImageSourceTest {

    @Test
    fun `photos are oriented by their exif data`() = runTest {
        val photo = File(requireNotNull(javaClass.getResource("/exif-rotated.jpg")).toURI())

        openImageSource(PlatformFile(photo)).use { source ->
            assertEquals(listOf(Size(100f, 200f)), source.pageSizes)
            val image = source.render(0, Rect(0f, 0f, 100f, 200f), IntSize(100, 200)).toAwtImage()
            assertEquals(Color.BLACK.rgb, image.getRGB(50, 20))
            assertEquals(Color.WHITE.rgb, image.getRGB(50, 150))
        }
    }
}
