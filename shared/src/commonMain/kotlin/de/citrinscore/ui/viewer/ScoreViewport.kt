package de.citrinscore.ui.viewer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import de.citrinscore.score.ScoreViewState

@Stable
class ScoreViewport(private val initial: ScoreViewState?) {
    var zoom by mutableFloatStateOf((initial?.zoom ?: 1f).coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    private var viewportSize by mutableStateOf(Size.Zero)
    private var documentSize by mutableStateOf(Size.Zero)

    var breaks: List<Float> = listOf(0f)

    private val maxOffsetY get() = (documentSize.height * zoom - viewportSize.height).coerceAtLeast(0f)

    val canGoToPreviousPage get() = offset.y > 0f
    val canGoToNextPage get() = offset.y < maxOffsetY

    val view: ScoreViewState?
        get() = if (documentSize.isEmpty()) initial else ScoreViewState(
            zoom = zoom,
            x = offset.x / (documentSize.width * zoom),
            y = offset.y / (documentSize.height * zoom),
        )

    fun setBounds(viewport: Size, document: Size) {
        if (viewport == viewportSize && document == documentSize) return
        val target = view
        viewportSize = viewport
        documentSize = document
        offset = clamp(target?.let { Offset(it.x * document.width * zoom, it.y * document.height * zoom) } ?: Offset.Zero)
    }

    fun scrollBy(delta: Offset): Offset {
        val previous = offset
        offset = clamp(offset + delta)
        return offset - previous
    }

    fun previousPage() {
        val top = offset.y
        val limit = top - viewportSize.height
        val target = scaledBreaks.firstOrNull { it >= limit && it < top - BREAK_TOLERANCE } ?: limit
        scrollBy(Offset(0f, target - top))
    }

    fun nextPage() {
        val top = offset.y
        val bottom = top + viewportSize.height
        val target = scaledBreaks.lastOrNull { it > top + BREAK_TOLERANCE && it <= bottom } ?: bottom
        scrollBy(Offset(0f, target - top))
    }

    fun reveal(document: Rect, ahead: Rect? = null) {
        val bounds = Rect(document.left * zoom, document.top * zoom, document.right * zoom, document.bottom * zoom)
        val visible = Rect(offset, viewportSize)
        val bottom = maxOf(bounds.bottom, (ahead?.bottom ?: 0f) * zoom)
        val x = if (bounds.left < visible.left || bounds.right > visible.right) bounds.center.x - viewportSize.width / 2 else offset.x
        val y = if (bounds.top < visible.top || bottom > visible.bottom) bounds.top - viewportSize.height * REVEAL_MARGIN else offset.y
        offset = clamp(Offset(x, y))
    }

    fun zoomBy(factor: Float, focus: Offset = viewportCenter) = zoomTo(zoom * factor, focus)

    fun zoomTo(target: Float, focus: Offset = viewportCenter) {
        val clamped = target.coerceIn(MIN_ZOOM, MAX_ZOOM)
        val ratio = clamped / zoom
        zoom = clamped
        offset = clamp((offset + focus) * ratio - focus)
    }

    private val viewportCenter get() = Offset(viewportSize.width / 2, viewportSize.height / 2)

    private val scaledBreaks get() = breaks.map { it * zoom }

    private fun clamp(offset: Offset): Offset {
        val width = documentSize.width * zoom
        val x = if (width <= viewportSize.width) (width - viewportSize.width) / 2 else offset.x.coerceIn(0f, width - viewportSize.width)
        val y = offset.y.coerceIn(0f, maxOffsetY)
        return Offset(x, y)
    }

    companion object {
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 5f
        const val ZOOM_STEP = 1.25f
        private const val BREAK_TOLERANCE = 1f
        private const val REVEAL_MARGIN = 0.05f
    }
}
