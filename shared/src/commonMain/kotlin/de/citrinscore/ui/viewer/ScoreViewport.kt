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
    private var glideTarget: Offset? = null

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

    /**
     * Follows the played system with the same glide as [step]. Once the system is the second to last one that fits,
     * because the system after [ahead] would not fit anymore, it glides down by at most half the visible height, but
     * never further than the system's top, so the played notes stay around the middle. A system that is not visible
     * at all is brought to the top. While a glide is running, the decision is made for where it ends, so a glide that
     * already heads for the system keeps running undisturbed.
     */
    suspend fun reveal(document: Rect, ahead: Rect? = null) {
        val settled = glideTarget ?: offset
        val bounds = document.scaled(zoom)
        val next = ahead?.scaled(zoom)
        val visible = Rect(settled, Size(viewportSize.width, visibleHeight))
        val top = bounds.top - visibleHeight * REVEAL_MARGIN
        val x = if (bounds.left < visible.left || bounds.right > visible.right) bounds.center.x - viewportSize.width / 2 else settled.x
        val y = when {
            bounds.top < visible.top || bounds.bottom > visible.bottom -> top
            next != null && next.bottom + (next.bottom - bounds.bottom) > visible.bottom ->
                settled.y + minOf(visibleHeight * STEP_FRACTION, top - settled.y).coerceAtLeast(0f)
            else -> settled.y
        }
        val target = clamp(Offset(x, y))
        if (target != settled) glideTo(target)
    }

    private fun Rect.scaled(factor: Float) = Rect(left * factor, top * factor, right * factor, bottom * factor)

    private suspend fun glideTo(target: Offset) = stepping.mutate {
        glideTarget = target
        val distance = target - offset
        var travelled = Offset.Zero
        try {
            animate(0f, 1f, animationSpec = tween(STEP_MILLIS, easing = FastOutSlowInEasing)) { fraction, _ ->
                val position = distance * fraction
                offset = clamp(offset + position - travelled)
                travelled = position
            }
        } finally {
            if (glideTarget == target) glideTarget = null
        }
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
