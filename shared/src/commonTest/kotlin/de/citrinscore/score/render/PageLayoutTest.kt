package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlin.test.Test
import kotlin.test.assertEquals

class PageLayoutTest {

    @Test
    fun `pages share one scale and are stacked left aligned with gaps`() {
        val layout = PageLayout(listOf(IntSize(2000, 400), IntSize(1000, 400)), width = 1010f, margin = 5f, gap = 10f)

        assertEquals(0.5f, layout.scale)
        assertEquals(listOf(Rect(5f, 5f, 1005f, 205f), Rect(5f, 215f, 505f, 415f)), layout.pages)
        assertEquals(420f, layout.height)
    }
}
