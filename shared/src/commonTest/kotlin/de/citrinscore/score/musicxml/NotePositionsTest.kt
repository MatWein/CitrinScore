package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotePositionsTest {

    private val positions = NotePositions(
        listOf(
            EngravedPage(
                svg = "",
                staves = listOf(
                    StaffBox(0, 0, Rect(100f, 100f, 500f, 172f), 18f),
                    StaffBox(0, 1, Rect(100f, 250f, 500f, 322f), 18f),
                ),
                notes = listOf(
                    NoteBox("upper", 0, Rect(200f, 120f, 222f, 138f)),
                    NoteBox("lower", 0, Rect(202f, 280f, 224f, 298f)),
                    NoteBox("next", 0, Rect(300f, 130f, 322f, 148f)),
                ),
            ),
        ),
    )

    @Test
    fun `the cursor spans the whole system at the notes`() {
        val cursor = positions.cursor(listOf("upper", "lower"))

        assertEquals(PageRegion(0, Rect(200f - 7.2f, 100f - 18f, 224f + 7.2f, 322f + 18f)), cursor)
    }

    @Test
    fun `taps select the nearest note close to the staff`() {
        assertEquals("next", positions.nearest(0, Offset(310f, 160f)))
        assertEquals("lower", positions.nearest(0, Offset(210f, 300f)))
        assertNull(positions.nearest(0, Offset(900f, 900f)))
        assertNull(positions.nearest(1, Offset(210f, 300f)))
    }
}
