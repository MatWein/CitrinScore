package de.citrinscore.ui.viewer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatorMutex
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.ScrollMode

@Stable
class ScoreViewport(private val initial: ScoreViewState?) {
    var zoom by mutableFloatStateOf((initial?.zoom ?: 1f).coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    private var viewportSize by mutableStateOf(Size.Zero)
    private var documentSize by mutableStateOf(Size.Zero)
    private var bottomInset by mutableFloatStateOf(0f)

    var scrollMode by mutableStateOf(ScrollMode.Vertical)

    private val stepping = MutatorMutex()
    private var remainingStep = 0f

    private val maxOffsetY get() = (documentSize.height * zoom + bottomInset - viewportSize.height).coerceAtLeast(0f)

    private val visibleHeight get() = (viewportSize.height - bottomInset).coerceAtLeast(0f)

    val canScrollDown get() = offset.y < maxOffsetY - EDGE_TOLERANCE

    val view: ScoreViewState?
        get() = if (documentSize.isEmpty()) initial else ScoreViewState(
            zoom = zoom,
            x = offset.x / (documentSize.width * zoom),
            y = offset.y / (documentSize.height * zoom),
        )

    fun setBounds(viewport: Size, document: Size, bottomInset: Float = 0f) {
        if (viewport == viewportSize && document == documentSize && bottomInset == this.bottomInset) return
        val target = view
        viewportSize = viewport
        documentSize = document
        this.bottomInset = bottomInset
        offset = clamp(target?.let { Offset(it.x * document.width * zoom, it.y * document.height * zoom) } ?: Offset.Zero)
    }

    fun scrollBy(delta: Offset): Offset {
        val allowed = when (scrollMode) {
            ScrollMode.Both -> delta
            ScrollMode.Vertical -> Offset(0f, delta.y)
            ScrollMode.Horizontal -> Offset(delta.x, 0f)
        }
        val previous = offset
        offset = clamp(offset + allowed)
        return offset - previous
    }

    /** Glides by half the visible height per step; steps requested while one is running add up. */
    suspend fun step(direction: Int) {
        val requested = remainingStep + direction * visibleHeight * STEP_FRACTION
        val distance = requested.coerceIn(-offset.y, maxOffsetY - offset.y)
        if (distance == 0f) return
        stepping.mutate {
            remainingStep = distance
            var travelled = 0f
            try {
                animate(0f, distance, animationSpec = tween(STEP_MILLIS, easing = FastOutSlowInEasing)) { value, _ ->
                    offset = clamp(offset + Offset(0f, value - travelled))
                    travelled = value
                    remainingStep = distance - value
                }
            } finally {
                remainingStep = 0f
            }
        }
    }

    fun reveal(document: Rect, ahead: Rect? = null) {
        val bounds = Rect(document.left * zoom, document.top * zoom, document.right * zoom, document.bottom * zoom)
        val visible = Rect(offset, Size(viewportSize.width, visibleHeight))
        val bottom = maxOf(bounds.bottom, (ahead?.bottom ?: 0f) * zoom)
        val x = if (bounds.left < visible.left || bounds.right > visible.right) bounds.center.x - viewportSize.width / 2 else offset.x
        val y = if (bounds.top < visible.top || bottom > visible.bottom) bounds.top - visibleHeight * REVEAL_MARGIN else offset.y
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
        private const val STEP_FRACTION = 0.5f
        private const val EDGE_TOLERANCE = 0.5f
        private const val STEP_MILLIS = 700
        private const val REVEAL_MARGIN = 0.05f
    }
}
