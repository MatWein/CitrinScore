package de.citrinscore.ui.pages

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.PageCrop
import de.citrinscore.score.render.PageSource
import de.citrinscore.score.render.contentCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

const val PREVIEW_WIDTH = 1000

data class PreviewPage(val image: ImageBitmap, val crop: PageCrop)

fun previewSize(width: Float, height: Float) = IntSize(PREVIEW_WIDTH, (PREVIEW_WIDTH * height / width).roundToInt().coerceAtLeast(1))

suspend fun PageSource.previewPage(page: Int): PreviewPage {
    val crop = contentCrop(page)
    val size = previewSize(crop.size.width, crop.size.height)
    return PreviewPage(withContext(Dispatchers.Default) { render(page, crop.rect, size) }, crop)
}
