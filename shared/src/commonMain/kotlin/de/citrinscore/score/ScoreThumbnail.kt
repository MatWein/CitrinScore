package de.citrinscore.score

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.render.PageLayout
import de.citrinscore.score.render.ScoreRenderer
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import kotlin.math.min
import kotlin.math.roundToInt

object ScoreThumbnail {
    private const val WIDTH = CitScore.THUMBNAIL_WIDTH
    private const val MARGIN = 6f
    private const val GAP = 12f
    private const val JPEG_QUALITY = 85
    private val height = (WIDTH / CitScore.PAGE_ASPECT_RATIO).roundToInt()

    suspend fun render(renderer: ScoreRenderer): ByteArray {
        val thumbnail = ImageBitmap(WIDTH, height)
        val canvas = Canvas(thumbnail)
        canvas.drawRect(0f, 0f, WIDTH.toFloat(), height.toFloat(), Paint().apply { color = Color.White })
        val layout = PageLayout(renderer.pageSizes, WIDTH.toFloat(), MARGIN, GAP)
        for ((index, target) in layout.pages.withIndex()) {
            if (target.top >= height) break
            val visibleHeight = min(target.bottom, height.toFloat()) - target.top
            val size = IntSize(target.width.roundToInt(), visibleHeight.roundToInt())
            if (size.width <= 0 || size.height <= 0) continue
            val region = Rect(0f, 0f, renderer.pageSizes[index].width, visibleHeight / layout.scale)
            val page = renderer.render(index, region, size)
            canvas.drawImage(page, Offset(target.left.roundToInt().toFloat(), target.top.roundToInt().toFloat()), Paint())
        }
        return thumbnail.encodeToByteArray(ImageFormat.JPEG, JPEG_QUALITY)
    }
}
