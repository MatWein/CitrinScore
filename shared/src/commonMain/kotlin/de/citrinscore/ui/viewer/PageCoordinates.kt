package de.citrinscore.ui.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.citrinscore.score.render.PageLayout

class PagePoint(val page: Int, val position: Offset)

fun PageLayout.pageAt(screen: Offset, viewport: ScoreViewport): Int? {
    val document = (screen + viewport.offset) / viewport.zoom
    return pages.indexOfFirst { it.contains(document) }.takeIf { it >= 0 }
}

fun PageLayout.pagePoint(screen: Offset, viewport: ScoreViewport): PagePoint? =
    pageAt(screen, viewport)?.let { PagePoint(it, pagePosition(it, screen, viewport)) }

fun PageLayout.pagePosition(page: Int, screen: Offset, viewport: ScoreViewport): Offset {
    val document = (screen + viewport.offset) / viewport.zoom
    return Offset((document.x - pages[page].left) / scale, (document.y - pages[page].top) / scale)
}

fun PageLayout.screenPoint(page: Int, point: Offset, zoom: Float, offset: Offset) =
    Offset((pages[page].left + point.x * scale) * zoom - offset.x, (pages[page].top + point.y * scale) * zoom - offset.y)

fun PageLayout.documentRect(page: Int, rect: Rect) =
    Rect(pages[page].left + rect.left * scale, pages[page].top + rect.top * scale, pages[page].left + rect.right * scale, pages[page].top + rect.bottom * scale)
