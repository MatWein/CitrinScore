package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Rect
import de.citrinscore.score.InkStroke
import de.citrinscore.score.MeasureAnchor
import de.citrinscore.score.TextNote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class MeasureAnchorsTest {

    private val wide = MeasureAnchors(
        listOf(
            listOf(
                StaffBox(0, 0, Rect(100f, 100f, 500f, 172f), 18f),
                StaffBox(1, 0, Rect(500f, 100f, 900f, 172f), 18f),
            ),
        ),
    )
    private val narrow = MeasureAnchors(
        listOf(
            listOf(StaffBox(0, 0, Rect(50f, 80f, 650f, 116f), 9f)),
            listOf(StaffBox(1, 0, Rect(50f, 40f, 450f, 76f), 9f)),
        ),
    )

    @Test
    fun `strokes keep their position relative to the measure in another layout`() {
        val anchored = wide.anchor(InkStroke(page = 0, points = listOf(200f, 82f, 300f, 136f), width = 9f)).single()

        assertEquals(MeasureAnchor(0, 0), anchored.anchor)
        assertEquals(InkStroke(page = 0, points = listOf(200f, 71f, 350f, 98f), width = 4.5f), narrow.project(anchored))
    }

    @Test
    fun `strokes crossing a barline are split and follow both measures`() {
        val anchored = wide.anchor(InkStroke(page = 0, points = listOf(400f, 136f, 600f, 136f, 700f, 136f), width = 9f))

        assertEquals(listOf(MeasureAnchor(0, 0), MeasureAnchor(1, 0)), anchored.map { it.anchor })
        assertEquals(listOf(0, 1), anchored.map { assertNotNull(narrow.project(it)).page })
        val first = assertNotNull(narrow.project(anchored[0])).points
        val second = assertNotNull(narrow.project(anchored[1])).points
        assertEquals(650f, first[first.size - 2], absoluteTolerance = 0.1f)
        assertEquals(50f, second[0], absoluteTolerance = 0.1f)
        assertEquals(listOf(150f, 58f, 250f, 58f), second.drop(2))
    }

    @Test
    fun `positions are measured from the first note so clefs at the start of a system do not shift them`() {
        val withClef = MeasureAnchors(listOf(listOf(StaffBox(0, 0, Rect(0f, 100f, 600f, 172f), 18f, contentLeft = 200f))))
        val anchored = withClef.anchor(InkStroke(page = 0, points = listOf(300f, 100f), width = 9f)).single()

        assertEquals(listOf(200f, 100f), assertNotNull(wide.project(anchored)).points)
    }

    @Test
    fun `texts are anchored to the nearest staff`() {
        val anchored = assertNotNull(wide.anchor(TextNote(page = 0, x = 900f, y = 10f, text = "p", size = 36f)))

        assertEquals(MeasureAnchor(1, 0), anchored.anchor)
        assertEquals(TextNote(page = 1, x = 450f, y = -5f, text = "p", size = 18f), narrow.project(anchored))
    }
}
