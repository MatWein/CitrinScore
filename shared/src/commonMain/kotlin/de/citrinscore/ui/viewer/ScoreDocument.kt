package de.citrinscore.ui.viewer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.rememberScrollable2DState
import androidx.compose.foundation.gestures.scrollable2D
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toOffset
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.render.PageLayout
import de.citrinscore.ui.viewer.annotations.AnnotationGesture
import de.citrinscore.ui.viewer.annotations.AnnotationInput
import de.citrinscore.ui.viewer.annotations.annotationColor
import de.citrinscore.ui.viewer.annotations.annotationInput
import de.citrinscore.ui.viewer.annotations.drawAnnotations
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.roundToInt

private val Margin = 4.dp
private val PageGap = 8.dp
private val WheelStep = 64.dp
private const val SETTLE_MILLIS = 150L
private const val PINCH_POINTERS = 2

typealias PageRenderer = suspend (page: Int, region: Rect, size: IntSize) -> ImageBitmap?

private class Tile(val image: ImageBitmap, val documentRect: Rect)

private const val MAX_CHANNEL = 255f
private const val RED_LUMINANCE = 0.299f
private const val GREEN_LUMINANCE = 0.587f
private const val BLUE_LUMINANCE = 0.114f

class PaperColors(val paper: Color, val ink: Color) {
    val filter = ColorFilter.colorMatrix(
        ColorMatrix(
            channel(ink.red, paper.red) + channel(ink.green, paper.green) + channel(ink.blue, paper.blue) +
                floatArrayOf(0f, 0f, 0f, 1f, 0f),
        ),
    )

    private fun channel(ink: Float, paper: Float): FloatArray {
        val range = paper - ink
        return floatArrayOf(range * RED_LUMINANCE, range * GREEN_LUMINANCE, range * BLUE_LUMINANCE, 0f, ink * MAX_CHANNEL)
    }
}

@Composable
fun ScoreDocument(
    pageSizes: List<Size>,
    systemBreaks: List<List<Float>>,
    viewport: ScoreViewport,
    render: PageRenderer,
    paperColors: PaperColors?,
    annotations: ScoreAnnotations,
    annotationInput: AnnotationInput?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier.fillMaxSize().clipToBounds()) {
        val density = LocalDensity.current
        val viewportSize = Size(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat())
        val layout = remember(pageSizes, viewportSize.width, density) {
            with(density) { PageLayout(pageSizes, viewportSize.width, Margin.toPx(), PageGap.toPx()) }
        }
        val breaks = remember(layout, systemBreaks) { layout.documentBreaks(systemBreaks) }
        SideEffect {
            viewport.setBounds(viewportSize, Size(layout.width, layout.height))
            viewport.breaks = breaks
        }
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
                        render(page, Rect(Offset.Zero, pageSizes[page]), size)?.let { baseImages[page] = it }
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
        val textMeasurer = rememberTextMeasurer()
        val gesture = remember { AnnotationGesture() }
        val colorFilter = paperColors?.filter
        val inkColor = annotationColor(inverted = paperColors != null)
        Canvas(
            Modifier
                .fillMaxSize()
                .pinchToZoom(viewport)
                .wheelScroll(viewport, wheelStep)
                .then(
                    if (annotationInput == null) {
                        Modifier.mouseDrag(viewport).scrollable2D(scrollState)
                    } else {
                        Modifier.annotationInput(annotationInput, layout, viewport, annotations, gesture, textMeasurer)
                    },
                ),
        ) {
            val zoom = viewport.zoom
            val offset = viewport.offset.round()
            layout.visiblePages(zoom, offset, size, preload = 0f).forEach { page ->
                baseImages[page]?.let { drawAt(it, layout.pages[page].scale(zoom).roundToIntRect().translate(-offset), colorFilter) }
            }
            tiles.forEach { tile -> drawAt(tile.image, tile.documentRect.scale(zoom).roundToIntRect().translate(-offset), colorFilter) }
            drawAnnotations(annotations, gesture, layout, zoom, offset.toOffset(), inkColor, textMeasurer)
        }
    }
}

private fun DrawScope.drawAt(image: ImageBitmap, target: IntRect, colorFilter: ColorFilter?) =
    drawImage(image, dstOffset = target.topLeft, dstSize = target.size, colorFilter = colorFilter)

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
            if (event.changes.count { it.pressed } >= PINCH_POINTERS) {
                viewport.zoomBy(event.calculateZoom(), event.calculateCentroid())
                viewport.scrollBy(-event.calculatePan())
                event.changes.forEach { it.consume() }
            }
        } while (event.changes.any { it.pressed })
    }
}

private fun Modifier.mouseDrag(viewport: ScoreViewport) = pointerInput(viewport) {
    awaitEachGesture {
        val down = awaitFirstDown()
        if (down.type != PointerType.Mouse) return@awaitEachGesture
        drag(down.id) { change ->
            viewport.scrollBy(-change.positionChange())
            change.consume()
        }
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
