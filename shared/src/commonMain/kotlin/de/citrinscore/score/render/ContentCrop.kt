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

suspend fun PageSource.contentCrop(page: Int, within: PageCrop? = null): PageCrop = withContext(Dispatchers.Default) {
    val size = pageSizes[page]
    val area = within ?: PageCrop(0f, 0f, size.width, size.height)
    val scale = SCAN_WIDTH / size.width
    val bounds = render(page, area.rect, scanSize(area.size, scale)).let(::InkScan).inkBounds() ?: return@withContext area
    val padding = size.width * PADDING_FRACTION
    PageCrop(
        left = max(area.left, floor(area.left + bounds.left / scale - padding)),
        top = max(area.top, floor(area.top + bounds.top / scale - padding)),
        right = min(area.right, ceil(area.left + bounds.right / scale + padding)),
        bottom = min(area.bottom, ceil(area.top + bounds.bottom / scale + padding)),
    )
}

private fun InkScan.inkBounds(): Rect? {
    if (inkRows.isEmpty() || inkColumns.isEmpty()) return null
    return Rect(inkColumns.first().toFloat(), inkRows.first().toFloat(), inkColumns.last() + 1f, inkRows.last() + 1f)
}
