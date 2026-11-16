package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize

class PageLayout(pageSizes: List<IntSize>, val width: Float, margin: Float, gap: Float) {
    val scale = (width - 2 * margin) / (pageSizes.maxOfOrNull { it.width } ?: 1).coerceAtLeast(1)

    val pages: List<Rect>

    val height: Float

    init {
        var top = margin
        pages = pageSizes.map { size ->
            Rect(margin, top, margin + size.width * scale, top + size.height * scale).also { top = it.bottom + gap }
        }
        height = (pages.lastOrNull()?.bottom ?: 0f) + margin
    }
}
