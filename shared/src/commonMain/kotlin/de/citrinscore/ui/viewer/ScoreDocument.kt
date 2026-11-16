package de.citrinscore.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.rememberScrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toSize
import de.citrinscore.score.render.PageLayout
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

private val Margin = 4.dp
private val PageGap = 8.dp
private val WheelStep = 64.dp
private const val SETTLE_MILLIS = 150L

typealias PageRenderer = suspend (page: Int, region: Rect, size: IntSize) -> ImageBitmap?

private class Tile(val image: ImageBitmap, val documentRect: Rect)

@Composable
fun ScoreDocument(pageSizes: List<IntSize>, viewport: ScoreViewport, render: PageRenderer, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val viewportSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val layout = remember(pageSizes, viewportSize.width, density) {
            with(density) { PageLayout(pageSizes, viewportSize.width, Margin.toPx(), PageGap.toPx()) }
        }
        viewport.setBounds(viewportSize, Size(layout.width, layout.height))
        val baseImages = remember(layout) { mutableStateMapOf<Int, ImageBitmap>() }
        var tiles by remember(layout) { mutableStateOf(emptyList<Tile>()) }

        LaunchedEffect(layout, viewportSize) {
            snapshotFlow { layout.visiblePages(viewport.zoom, viewport.offset.round(), viewportSize, preload = viewportSize.height) }
                .distinctUntilChanged()
                .collectLatest { pages ->
                    baseImages.keys.retainAll(pages)
                    pages.filterNot(baseImages::containsKey).forEach { page ->
                        val target = layout.pages[page]
                        val size = IntSize(target.width.roundToInt(), target.height.roundToInt())
                        render(page, Rect(Offset.Zero, pageSizes[page].toSize()), size)?.let { baseImages[page] = it }
                    }
                }
        }
        LaunchedEffect(layout, viewportSize) {
            snapshotFlow { viewport.zoom to viewport.offset.round() }.collectLatest { (zoom, offset) ->
                if (zoom == 1f) {
                    tiles = emptyList()
                    return@collectLatest
                }
                delay(SETTLE_MILLIS)
                tiles = layout.visiblePages(zoom, offset, viewportSize, preload = 0f).mapNotNull { page ->
                    layout.detailTile(page, zoom, offset, viewportSize, render)
                }
            }
        }

        val scrollState = rememberScrollable2DState { delta -> -viewport.scrollBy(-delta) }
        val wheelStep = with(density) { WheelStep.toPx() }
        Canvas(
            Modifier
                .fillMaxSize()
                .pinchToZoom(viewport)
                .wheelScroll(viewport, wheelStep)
                .scrollable2D(scrollState),
        ) {
            val zoom = viewport.zoom
            val offset = viewport.offset.round()
            layout.visiblePages(zoom, offset, size, preload = 0f).forEach { page ->
                baseImages[page]?.let { drawAt(it, layout.pages[page].scale(zoom).roundToIntRect().translate(-offset)) }
            }
            tiles.forEach { tile -> drawAt(tile.image, tile.documentRect.scale(zoom).roundToIntRect().translate(-offset)) }
        }
    }
}

private fun DrawScope.drawAt(image: ImageBitmap, target: IntRect) =
    drawImage(image, dstOffset = target.topLeft, dstSize = target.size)

private fun PageLayout.visiblePages(zoom: Float, offset: IntOffset, viewport: Size, preload: Float): List<Int> {
    val top = (offset.y - preload) / zoom
    val bottom = (offset.y + viewport.height + preload) / zoom
    return pages.indices.filter { pages[it].bottom >= top && pages[it].top <= bottom }
}

private suspend fun PageLayout.detailTile(page: Int, zoom: Float, offset: IntOffset, viewport: Size, render: PageRenderer): Tile? {
    val target = pages[page]
    val onScreen = target.scale(zoom).translate(-offset.x.toFloat(), -offset.y.toFloat()).intersect(Rect(Offset.Zero, viewport))
    if (onScreen.width < 1f || onScreen.height < 1f) return null
    val screen = onScreen.roundToIntRect()
    val documentRect = Rect(screen.left.toFloat(), screen.top.toFloat(), screen.right.toFloat(), screen.bottom.toFloat())
        .translate(offset.x.toFloat(), offset.y.toFloat())
        .scale(1 / zoom)
    val region = documentRect.translate(-target.left, -target.top).scale(1 / scale)
    return render(page, region, screen.size)?.let { Tile(it, documentRect) }
}

private fun Rect.scale(factor: Float) = Rect(left * factor, top * factor, right * factor, bottom * factor)

private fun Modifier.pinchToZoom(viewport: ScoreViewport) = pointerInput(viewport) {
    awaitEachGesture {
        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        do {
            val event = awaitPointerEvent(PointerEventPass.Initial)
            if (event.changes.count { it.pressed } >= 2) {
                viewport.zoomBy(event.calculateZoom(), event.calculateCentroid())
                viewport.scrollBy(-event.calculatePan())
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun Modifier.wheelScroll(viewport: ScoreViewport, step: Float) = pointerInput(viewport) {
    awaitPointerEventScope {
        while (true) {
            val event = awaitPointerEvent()
            if (event.type != PointerEventType.Scroll) continue
            val change = event.changes.first()
            val delta = change.scrollDelta
            when {
                event.keyboardModifiers.isCtrlPressed ->
                    if (delta.y != 0f) viewport.zoomBy(if (delta.y < 0) ScoreViewport.ZOOM_STEP else 1 / ScoreViewport.ZOOM_STEP, change.position)
                event.keyboardModifiers.isShiftPressed && delta.x == 0f -> viewport.scrollBy(Offset(delta.y, 0f) * step)
                else -> viewport.scrollBy(delta * step)
            }
            change.consume()
        }
    }
}
