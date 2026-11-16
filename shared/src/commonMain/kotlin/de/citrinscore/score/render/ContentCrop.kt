package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import de.citrinscore.score.PageCrop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private const val PADDING_FRACTION = 0.01f

suspend fun PageSource.contentCrop(page: Int): PageCrop = withContext(Dispatchers.Default) {
    val size = pageSizes[page]
    val full = PageCrop(0f, 0f, size.width, size.height)
    val scale = SCAN_WIDTH / size.width
    val bounds = render(page, full.rect, scanSize(size, scale)).let(::InkScan).inkBounds() ?: return@withContext full
    val padding = size.width * PADDING_FRACTION
    PageCrop(
        left = max(0f, floor(bounds.left / scale - padding)),
        top = max(0f, floor(bounds.top / scale - padding)),
        right = min(size.width, ceil(bounds.right / scale + padding)),
        bottom = min(size.height, ceil(bounds.bottom / scale + padding)),
    )
}

private fun InkScan.inkBounds(): Rect? {
    if (inkRows.isEmpty() || inkColumns.isEmpty()) return null
    return Rect(inkColumns.first().toFloat(), inkRows.first().toFloat(), inkColumns.last() + 1f, inkRows.last() + 1f)
}
