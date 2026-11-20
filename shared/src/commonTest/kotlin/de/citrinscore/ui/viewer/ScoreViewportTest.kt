package de.citrinscore.ui.viewer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import de.citrinscore.score.ScoreViewState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ScoreViewportTest {

    private val viewportSize = Size(1000f, 800f)
    private val documentSize = Size(1000f, 3000f)

    @Test
    fun `zooming keeps the focus point in place`() {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize) }

        viewport.zoomBy(2f, Offset(500f, 400f))

        assertEquals(2f, viewport.zoom)
        assertEquals(Offset(500f, 400f), viewport.offset)
    }

    @Test
    fun `scrolling stops at the document edges`() {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize) }

        val consumed = viewport.scrollBy(Offset(300f, 5000f))

        assertEquals(Offset(0f, 2200f), consumed)
        assertEquals(Offset(0f, 2200f), viewport.offset)
    }

    @Test
    fun `the bottom inset adds room below the document and is treated as hidden`() {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize, bottomInset = 100f) }

        viewport.scrollBy(Offset(0f, 5000f))
        assertEquals(2300f, viewport.offset.y)

        viewport.scrollBy(Offset(0f, -5000f))
        viewport.nextPage()
        assertEquals(700f, viewport.offset.y)

        viewport.scrollBy(Offset(0f, -5000f))
        viewport.reveal(Rect(0f, 750f, 100f, 780f))
        assertEquals(750f - 700f * 0.05f, viewport.offset.y)
    }

    @Test
    fun `a document narrower than the viewport is centered`() {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize) }

        viewport.zoomBy(0.5f, Offset.Zero)

        assertEquals(-250f, viewport.offset.x)
    }

    @Test
    fun `a saved view is restored independent of the viewport size`() {
        val original = ScoreViewport(null).apply {
            setBounds(viewportSize, documentSize)
            zoomBy(2f, Offset.Zero)
            scrollBy(Offset(400f, 1500f))
        }

        val restored = ScoreViewport(original.view).apply { setBounds(Size(500f, 400f), Size(500f, 1500f)) }

        assertEquals(ScoreViewState(2f, 0.2f, 0.25f), original.view)
        assertEquals(Offset(200f, 750f), restored.offset)
    }

    @Test
    fun `paging moves by exactly one viewport height`() {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize) }

        viewport.nextPage()
        assertEquals(Offset(0f, 800f), viewport.offset)

        viewport.nextPage()
        viewport.previousPage()
        assertEquals(Offset(0f, 800f), viewport.offset)
    }

    @Test
    fun `paging stops at the document edges`() {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize) }

        assertFalse(viewport.canGoToPreviousPage)
        assertTrue(viewport.canGoToNextPage)

        repeat(3) { viewport.nextPage() }

        assertEquals(Offset(0f, 2200f), viewport.offset)
        assertTrue(viewport.canGoToPreviousPage)
        assertFalse(viewport.canGoToNextPage)
    }

    @Test
    fun `paging keeps a partly visible system for the next page`() {
        val viewport = ScoreViewport(null).apply {
            setBounds(viewportSize, documentSize)
            breaks = listOf(0f, 300f, 600f, 900f, 1200f, 1500f)
        }

        viewport.nextPage()
        assertEquals(600f, viewport.offset.y)

        viewport.nextPage()
        assertEquals(1200f, viewport.offset.y)

        viewport.previousPage()
        assertEquals(600f, viewport.offset.y)
    }

    @Test
    fun `paging scales the breaks with the zoom`() {
        val viewport = ScoreViewport(null).apply {
            setBounds(viewportSize, documentSize)
            breaks = listOf(0f, 300f, 600f)
            zoomTo(2f, Offset.Zero)
        }

        viewport.nextPage()

        assertEquals(600f, viewport.offset.y)
    }

    @Test
    fun `paging falls back to the viewport height without a break in view`() {
        val viewport = ScoreViewport(null).apply {
            setBounds(viewportSize, documentSize)
            breaks = listOf(0f, 1000f)
        }

        viewport.nextPage()

        assertEquals(800f, viewport.offset.y)
    }

    @Test
    fun `revealing turns the page once the following system leaves the viewport`() {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize) }

        viewport.reveal(Rect(0f, 300f, 1000f, 500f), ahead = Rect(0f, 550f, 1000f, 750f))
        assertEquals(0f, viewport.offset.y)

        viewport.reveal(Rect(0f, 550f, 1000f, 750f), ahead = Rect(0f, 800f, 1000f, 1000f))
        assertEquals(510f, viewport.offset.y)
    }
}
