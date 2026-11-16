package de.citrinscore.score

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import org.jetbrains.compose.resources.decodeToImageBitmap
import kotlin.math.roundToInt

object ScoreThumbnail {
    private const val WIDTH = CitScore.THUMBNAIL_WIDTH
    private const val JPEG_QUALITY = 85
    private val height = (WIDTH / CitScore.PAGE_ASPECT_RATIO).roundToInt()

    suspend fun render(pages: List<PlatformFile>): ByteArray {
        val thumbnail = ImageBitmap(WIDTH, height)
        val canvas = Canvas(thumbnail)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), Paint().apply { color = Color.White })
        val paint = Paint().apply { filterQuality = FilterQuality.High }
        var top = 0
        for (page in pages) {
            if (top >= height) break
            val image = FileKit.compressImage(page, ImageFormat.PNG, maxWidth = WIDTH).decodeToImageBitmap()
            val scaledHeight = (image.height * WIDTH.toFloat() / image.width).roundToInt()
            canvas.drawImageRect(image, dstOffset = IntOffset(0, top), dstSize = IntSize(WIDTH, scaledHeight), paint = paint)
            top += scaledHeight
        }
        return thumbnail.encodeToByteArray(ImageFormat.JPEG, JPEG_QUALITY)
    }
}
