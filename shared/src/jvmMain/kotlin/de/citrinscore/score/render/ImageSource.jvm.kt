package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.Color
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.Image
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Paint
import org.jetbrains.skia.SamplingMode
import org.jetbrains.skia.impl.use
import org.jetbrains.skia.Rect as SkiaRect

actual suspend fun openRasterImageSource(file: PlatformFile): PageSource {
    val bytes = file.readBytes()
    return withContext(Dispatchers.Default) {
        Image.makeFromEncoded(bytes).use { encoded ->
            val pixels = Bitmap().apply { allocN32Pixels(encoded.width, encoded.height) }
            Canvas(pixels).use { it.drawImage(encoded, 0f, 0f) }
            SkiaImageSource(Image.makeFromBitmap(pixels.setImmutable()))
        }
    }
}

private class SkiaImageSource(private val image: Image) : PageSource {
    override val pageSizes = listOf(Size(image.width.toFloat(), image.height.toFloat()))

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap {
        val bitmap = Bitmap().apply { allocN32Pixels(size.width, size.height) }
        val sampling = if (size.width < region.width) FilterMipmap(FilterMode.LINEAR, MipmapMode.LINEAR) else SamplingMode.CATMULL_ROM
        Canvas(bitmap).use { canvas ->
            canvas.clear(Color.WHITE)
            canvas.drawImageRect(
                image,
                SkiaRect.makeLTRB(region.left, region.top, region.right, region.bottom),
                SkiaRect.makeWH(size.width.toFloat(), size.height.toFloat()),
                sampling,
                Paint(),
                true,
            )
        }
        return bitmap.setImmutable().asComposeImageBitmap()
    }

    override fun close() = image.close()
}
