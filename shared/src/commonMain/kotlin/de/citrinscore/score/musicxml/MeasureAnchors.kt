package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Offset
import de.citrinscore.score.InkStroke
import de.citrinscore.score.MeasureAnchor
import de.citrinscore.score.MeasureStroke
import de.citrinscore.score.MeasureText
import de.citrinscore.score.TextNote
import androidx.compose.ui.geometry.lerp
import kotlin.math.hypot
import kotlin.math.max

private const val BOUNDARY_STEPS = 16

class MeasureAnchors(private val pages: List<List<StaffBox>>) {
    private val placements = pages.flatMapIndexed { page, staves -> staves.map { MeasureAnchor(it.measure, it.staff) to Placement(page, it) } }.toMap()

    val staffSpace = pages.flatten().map { it.space }.sorted().let { it.getOrNull(it.size / 2) }

    fun staffSpace(page: Int) = pages.getOrNull(page)?.map { it.space }?.maxOrNull() ?: staffSpace

    fun anchor(stroke: InkStroke): List<MeasureStroke> {
        val segments = mutableListOf<Segment>()
        var previous: Offset? = null
        stroke.points.chunked(2) { (x, y) -> Offset(x, y) }.forEach { point ->
            val box = nearest(stroke.page, point) ?: return@forEach
            val current = segments.lastOrNull()
            val last = previous
            if (current?.box == box) {
                current.points += point
            } else if (current == null || last == null) {
                segments += Segment(box, mutableListOf(point))
            } else {
                val boundary = boundary(stroke.page, last, point, current.box)
                current.points += boundary
                segments += Segment(box, mutableListOf(boundary, point))
            }
            previous = point
        }
        return segments.map { (box, points) ->
            MeasureStroke(box.anchor, points.flatMap { box.local(it).let { local -> listOf(local.x, local.y) } }, stroke.width / box.space)
        }
    }

    fun anchor(note: TextNote): MeasureText? {
        val position = Offset(note.x, note.y)
        val box = nearest(note.page, position) ?: return null
        val local = box.local(position)
        return MeasureText(box.anchor, local.x, local.y, note.text, note.size / box.space)
    }

    fun project(stroke: MeasureStroke): InkStroke? {
        val (page, box) = placements[stroke.anchor] ?: return null
        val points = stroke.points.chunked(2) { (x, y) -> box.page(Offset(x, y)) }.flatMap { listOf(it.x, it.y) }
        return InkStroke(page, points, stroke.width * box.space)
    }

    fun project(text: MeasureText): TextNote? {
        val (page, box) = placements[text.anchor] ?: return null
        val position = box.page(Offset(text.x, text.y))
        return TextNote(page, position.x, position.y, text.text, text.size * box.space)
    }

    private fun boundary(page: Int, inside: Offset, outside: Offset, box: StaffBox): Offset {
        var low = 0f
        var high = 1f
        repeat(BOUNDARY_STEPS) {
            val middle = (low + high) / 2
            if (nearest(page, lerp(inside, outside, middle)) == box) low = middle else high = middle
        }
        return lerp(inside, outside, (low + high) / 2)
    }

    private fun nearest(page: Int, point: Offset): StaffBox? = pages.getOrNull(page)?.minByOrNull { it.distanceTo(point) }

    private data class Placement(val page: Int, val box: StaffBox)

    private data class Segment(val box: StaffBox, val points: MutableList<Offset>)

    private val StaffBox.anchor get() = MeasureAnchor(measure, staff)

    private val StaffBox.contentWidth get() = bounds.right - contentLeft

    private fun StaffBox.local(point: Offset) = Offset((point.x - contentLeft) / contentWidth, (point.y - bounds.top) / space)

    private fun StaffBox.page(local: Offset) = Offset(contentLeft + local.x * contentWidth, bounds.top + local.y * space)

    private fun StaffBox.distanceTo(point: Offset): Float {
        val dx = max(max(bounds.left - point.x, point.x - bounds.right), 0f)
        val dy = max(max(bounds.top - point.y, point.y - bounds.bottom), 0f)
        return hypot(dx, dy)
    }
}
