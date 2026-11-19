package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import de.citrinscore.score.RecognitionLayout
import kotlin.math.hypot
import kotlin.math.max

private const val CURSOR_PADDING = 0.4f
private const val SYSTEM_PADDING = 1f
private const val TAP_DISTANCE = 4f

data class PageRegion(val page: Int, val bounds: Rect)

class NotePositions(private val notesByPage: List<List<NoteBox>>, stavesByPage: List<List<StaffBox>>) {
    constructor(pages: List<EngravedPage>) : this(pages.map { it.notes }, pages.map { it.staves })

    private val notes = notesByPage.flatMapIndexed { page, boxes -> boxes.map { it.id to Placement(page, it) } }.toMap()
    private val systems = stavesByPage.map { staves ->
        staves.groupBy { it.measure }.mapValues { (_, measure) -> System(measure.map { it.bounds }.reduce { a, b -> a.union(b) }, measure.maxOf { it.space }) }
    }

    fun note(id: String): PageRegion? = notes[id]?.let { PageRegion(it.page, it.box.bounds) }

    fun cursor(ids: List<String>): PageRegion? {
        val placed = ids.mapNotNull(notes::get)
        val first = placed.firstOrNull() ?: return null
        val system = systems[first.page][first.box.measure] ?: return null
        val heads = placed.filter { it.page == first.page && it.box.measure == first.box.measure }.map { it.box.bounds }
        val padding = system.space * CURSOR_PADDING
        return PageRegion(
            first.page,
            Rect(heads.minOf { it.left } - padding, system.extended.top, heads.maxOf { it.right } + padding, system.extended.bottom),
        )
    }

    fun followingSystem(ids: List<String>): PageRegion? {
        val placement = ids.firstNotNullOfOrNull(notes::get) ?: return null
        val current = systems[placement.page][placement.box.measure] ?: return null
        val below = systems[placement.page].values.filter { it.bounds.top >= current.bounds.bottom }.minByOrNull { it.bounds.top }
        if (below != null) return PageRegion(placement.page, below.extended)
        val next = placement.page + 1
        return systems.getOrNull(next)?.values?.minByOrNull { it.bounds.top }?.let { PageRegion(next, it.extended) }
    }

    fun system(id: String): PageRegion? = notes[id]?.let { placement ->
        systems[placement.page][placement.box.measure]?.let { PageRegion(placement.page, it.bounds) }
    }

    fun nearest(page: Int, point: Offset): String? {
        val (box, distance) = notesByPage.getOrNull(page)?.map { it to it.bounds.distanceTo(point) }?.minByOrNull { it.second } ?: return null
        val space = systems[page][box.measure]?.space ?: return null
        return box.id.takeIf { distance <= space * TAP_DISTANCE }
    }

    private data class Placement(val page: Int, val box: NoteBox)

    private class System(val bounds: Rect, val space: Float) {
        val extended get() = Rect(bounds.left, bounds.top - space * SYSTEM_PADDING, bounds.right, bounds.bottom + space * SYSTEM_PADDING)
    }

    private fun Rect.union(other: Rect) = Rect(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))

    private fun Rect.distanceTo(point: Offset) = hypot(max(max(left - point.x, point.x - right), 0f), max(max(top - point.y, point.y - bottom), 0f))
}

fun RecognitionLayout.notePositions(pages: Int) = NotePositions(
    notesByPage = List(pages) { page -> notes.filter { it.page == page }.map { NoteBox(it.id, it.measure, Rect(it.left, it.top, it.right, it.bottom)) } },
    stavesByPage = List(pages) { page ->
        measures.filter { it.page == page }.map { StaffBox(it.measure, 1, Rect(it.left, it.top, it.right, it.bottom), it.space) }
    },
)
