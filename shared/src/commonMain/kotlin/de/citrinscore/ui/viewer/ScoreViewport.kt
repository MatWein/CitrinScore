package de.citrinscore.ui.viewer

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import de.citrinscore.score.ScoreViewState

@Stable
class ScoreViewport(private val initial: ScoreViewState?) {
    var zoom by mutableFloatStateOf((initial?.zoom ?: 1f).coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set
    var offset by mutableStateOf(Offset.Zero)
        private set

    private var viewportSize = Size.Zero
    private var documentSize = Size.Zero

    val canZoomIn get() = zoom < MAX_ZOOM
    val canZoomOut get() = zoom > MIN_ZOOM

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

    fun zoomBy(factor: Float, focus: Offset = Offset(viewportSize.width / 2, viewportSize.height / 2)) {
        val target = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val ratio = target / zoom
        zoom = target
        offset = clamp((offset + focus) * ratio - focus)
    }

    private fun clamp(offset: Offset): Offset {
        val width = documentSize.width * zoom
        val height = documentSize.height * zoom
        val x = if (width <= viewportSize.width) (width - viewportSize.width) / 2 else offset.x.coerceIn(0f, width - viewportSize.width)
        val y = if (height <= viewportSize.height) 0f else offset.y.coerceIn(0f, height - viewportSize.height)
        return Offset(x, y)
    }

    companion object {
        const val MIN_ZOOM = 0.5f
        const val MAX_ZOOM = 5f
        const val ZOOM_STEP = 1.25f
    }
}
