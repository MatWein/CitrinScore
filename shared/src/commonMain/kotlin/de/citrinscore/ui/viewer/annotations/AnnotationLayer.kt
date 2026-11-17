package de.citrinscore.ui.viewer.annotations

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import de.citrinscore.score.InkStroke
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.TextNote
import de.citrinscore.score.musicxml.MeasureAnchors
import de.citrinscore.score.render.PageLayout
import de.citrinscore.ui.viewer.ScoreViewport
import de.citrinscore.ui.viewer.pageAt
import de.citrinscore.ui.viewer.pagePoint
import de.citrinscore.ui.viewer.pagePosition
import de.citrinscore.ui.viewer.screenPoint

private const val STROKE_WIDTH_FRACTION = 0.003f
private const val TEXT_SIZE_FRACTION = 0.02f
private const val STROKE_WIDTH_SPACES = 0.35f
private const val TEXT_SIZE_SPACES = 2.4f
private val EraserRadius = 16.dp
private val InkColor = Color(0xFF1E5AA8)

class AnnotationInput(
    val tool: AnnotationTool,
    val onStrokes: (List<InkStroke>) -> Unit,
    val onErase: (strokes: Set<InkStroke>, texts: Set<TextNote>) -> Unit,
    val onTextTap: (TextDraft) -> Unit,
    val sizing: AnnotationSizing? = null,
)

class AnnotationSizing(val strokeWidth: (page: Int) -> Float, val textSize: (page: Int) -> Float)

fun PageLayout.pageSizing() = AnnotationSizing({ pageWidth(it) * STROKE_WIDTH_FRACTION }, { pageWidth(it) * TEXT_SIZE_FRACTION })

fun MeasureAnchors.staffSizing() = AnnotationSizing(
    strokeWidth = { (staffSpace(it) ?: 0f) * STROKE_WIDTH_SPACES },
    textSize = { (staffSpace(it) ?: 0f) * TEXT_SIZE_SPACES },
)

class InkPiece(val page: Int, val points: List<Offset>)

@Stable
class AnnotationGesture {
    var pieces by mutableStateOf(emptyList<InkPiece>())
        private set
    var erasedStrokes by mutableStateOf(emptySet<InkStroke>())
        private set
    var erasedTexts by mutableStateOf(emptySet<TextNote>())
        private set

    fun begin(page: Int, point: Offset) {
        pieces = listOf(InkPiece(page, listOf(point)))
    }

    fun extend(point: Offset) {
        val current = pieces.last()
        pieces = pieces.dropLast(1) + InkPiece(current.page, current.points + point)
    }

    fun continueOn(page: Int, endOfPrevious: Offset, startOfNext: List<Offset>) {
        extend(endOfPrevious)
        pieces = pieces + InkPiece(page, startOfNext)
    }

    fun erase(strokes: Collection<InkStroke>, texts: Collection<TextNote>) {
        erasedStrokes = erasedStrokes + strokes
        erasedTexts = erasedTexts + texts
    }

    fun clear() {
        pieces = emptyList()
        erasedStrokes = emptySet()
        erasedTexts = emptySet()
    }
}

fun annotationColor(inverted: Boolean) = if (inverted) Color(1f - InkColor.red, 1f - InkColor.green, 1f - InkColor.blue) else InkColor

fun Modifier.annotationInput(
    input: AnnotationInput,
    layout: PageLayout,
    viewport: ScoreViewport,
    annotations: ScoreAnnotations,
    gesture: AnnotationGesture,
    textMeasurer: TextMeasurer,
) = pointerInput(input.tool, layout, annotations) {
    awaitEachGesture {
        val down = awaitFirstDown()
        val start = layout.pagePoint(down.position, viewport) ?: return@awaitEachGesture
        val sizing = input.sizing ?: layout.pageSizing()
        down.consume()
        when (input.tool) {
            AnnotationTool.Pen -> {
                gesture.begin(start.page, start.position)
                var previous = down.position
                val completed = drag(down.id) { change ->
                    val current = gesture.pieces.last().page
                    val page = layout.pageAt(change.position, viewport) ?: current
                    val position = layout.pagePosition(current, change.position, viewport)
                    if (page == current) {
                        gesture.extend(position)
                    } else {
                        val startOfNext = listOf(previous, change.position).map { layout.pagePosition(page, it, viewport) }
                        gesture.continueOn(page, position, startOfNext)
                    }
                    previous = change.position
                    change.consume()
                }
                if (completed) {
                    val width = sizing.strokeWidth(start.page)
                    input.onStrokes(gesture.pieces.map { piece -> InkStroke(piece.page, piece.points.flatMap { listOf(it.x, it.y) }, width) })
                }
                gesture.clear()
            }
            AnnotationTool.Eraser -> {
                val eraseAt = { change: PointerInputChange ->
                    layout.pagePoint(change.position, viewport)?.let { point ->
                        val radius = EraserRadius.toPx() / (layout.scale * viewport.zoom)
                        gesture.erase(
                            annotations.strokes.filter { it.page == point.page && it.isNear(point.position, radius) },
                            annotations.texts.filter { it.page == point.page && textMeasurer.bounds(it, this).contains(point.position) },
                        )
                    }
                    change.consume()
                }
                eraseAt(down)
                if (drag(down.id, eraseAt)) input.onErase(gesture.erasedStrokes, gesture.erasedTexts)
                gesture.clear()
            }
            AnnotationTool.Text -> {
                waitForUpOrCancellation()?.let { up ->
                    up.consume()
                    val existing = annotations.texts.firstOrNull { it.page == start.page && textMeasurer.bounds(it, this).contains(start.position) }
                    val size = sizing.textSize(start.page)
                    input.onTextTap(TextDraft(start.page, start.position.x, start.position.y, size, existing))
                }
            }
        }
    }
}

fun DrawScope.drawAnnotations(
    annotations: ScoreAnnotations,
    gesture: AnnotationGesture,
    layout: PageLayout,
    zoom: Float,
    offset: Offset,
    color: Color,
    textMeasurer: TextMeasurer,
    sizing: AnnotationSizing,
) {
    val factor = layout.scale * zoom
    val toScreen = { page: Int, point: Offset -> layout.screenPoint(page, point, zoom, offset) }
    annotations.strokes.filterNot { it in gesture.erasedStrokes }.forEach { stroke ->
        drawInk(stroke.positions().map { toScreen(stroke.page, it) }, stroke.width * factor, color)
    }
    gesture.pieces.firstOrNull()?.let { first ->
        val width = sizing.strokeWidth(first.page) * factor
        gesture.pieces.forEach { piece -> drawInk(piece.points.map { toScreen(piece.page, it) }, width, color) }
    }
    annotations.texts.filterNot { it in gesture.erasedTexts }.forEach { note ->
        drawText(
            textMeasurer = textMeasurer,
            text = note.text,
            topLeft = toScreen(note.page, Offset(note.x, note.y)),
            style = TextStyle(color = color, fontSize = (note.size * factor).toSp()),
        )
    }
}

private fun DrawScope.drawInk(points: List<Offset>, width: Float, color: Color) {
    if (points.isEmpty()) return
    if (points.size == 1) {
        drawCircle(color, radius = width / 2, center = points.first())
        return
    }
    val path = Path().apply {
        moveTo(points.first().x, points.first().y)
        points.drop(1).forEach { lineTo(it.x, it.y) }
    }
    drawPath(path, color, style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

private fun PageLayout.pageWidth(page: Int) = pages[page].width / scale

private fun InkStroke.positions() = points.chunked(2) { (x, y) -> Offset(x, y) }

private fun InkStroke.isNear(point: Offset, radius: Float) = positions().any { (it - point).getDistance() <= radius + width / 2 }

private fun TextMeasurer.bounds(note: TextNote, density: Density): Rect {
    val size = measure(note.text, TextStyle(fontSize = with(density) { note.size.toSp() })).size
    return Rect(note.x, note.y, note.x + size.width, note.y + size.height)
}
