package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.math.hypot
import kotlin.math.max

private const val CURSOR_PADDING = 0.4f
private const val SYSTEM_PADDING = 1f
private const val TAP_DISTANCE = 4f

data class PageRegion(val page: Int, val bounds: Rect)

class NotePositions(pages: List<EngravedPage>) {
    private val notes = pages.flatMapIndexed { page, engraved -> engraved.notes.map { it.id to Placement(page, it) } }.toMap()
    private val notesByPage = pages.map { it.notes }
    private val systems = pages.mapIndexed { page, engraved ->
        engraved.staves.groupBy { it.measure }.mapValues { (_, staves) -> System(staves.map { it.bounds }.reduce { a, b -> a.union(b) }, staves.maxOf { it.space }) }
    }

    fun note(id: String): PageRegion? = notes[id]?.let { PageRegion(it.page, it.box.bounds) }

    fun cursor(ids: List<String>): PageRegion? {
        val placed = ids.mapNotNull(notes::get)
        val first = placed.firstOrNull() ?: return null
        val system = systems[first.page][first.box.measure] ?: return null
        val heads = placed.filter { it.page == first.page && it.box.measure == first.box.measure }.map { it.box.bounds }
        val padding = system.space * CURSOR_PADDING
        val extent = system.space * SYSTEM_PADDING
        return PageRegion(
            first.page,
            Rect(heads.minOf { it.left } - padding, system.bounds.top - extent, heads.maxOf { it.right } + padding, system.bounds.bottom + extent),
        )
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

    private class System(val bounds: Rect, val space: Float)

    private fun Rect.union(other: Rect) = Rect(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))

    private fun Rect.distanceTo(point: Offset) = hypot(max(max(left - point.x, point.x - right), 0f), max(max(top - point.y, point.y - bottom), 0f))
}
