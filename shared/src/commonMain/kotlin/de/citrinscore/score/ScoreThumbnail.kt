package de.citrinscore.score

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.render.ImageSource
import de.citrinscore.score.render.PageLayout
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import kotlin.math.min
import kotlin.math.roundToInt

object ScoreThumbnail {
    private const val WIDTH = CitScore.THUMBNAIL_WIDTH
    private const val MARGIN = 6f
    private const val GAP = 12f
    private const val JPEG_QUALITY = 85
    private val height = (WIDTH / CitScore.PAGE_ASPECT_RATIO).roundToInt()

    suspend fun render(pages: List<PlatformFile>, crops: List<PageCrop>): ByteArray {
        val thumbnail = ImageBitmap(WIDTH, height)
        val canvas = Canvas(thumbnail)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), Paint().apply { color = Color.White })
        val layout = PageLayout(crops.map { it.size }, WIDTH.toFloat(), MARGIN, GAP)
        for ((index, target) in layout.pages.withIndex()) {
            if (target.top >= height) break
            val visibleBottom = min(target.bottom, height.toFloat())
            val crop = crops[index]
            val region = Rect(
                crop.left.toFloat(),
                crop.top.toFloat(),
                crop.right.toFloat(),
                crop.top + (visibleBottom - target.top) / layout.scale,
            )
            val size = IntSize(target.width.roundToInt(), (visibleBottom - target.top).roundToInt())
            if (size.width <= 0 || size.height <= 0) continue
            val page = ImageSource(pages[index]).use { it.render(region, size) }
            canvas.drawImage(page, Offset(target.left.roundToInt().toFloat(), target.top.roundToInt().toFloat()), Paint())
        }
        return thumbnail.encodeToByteArray(ImageFormat.JPEG, JPEG_QUALITY)
    }
}
