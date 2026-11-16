package de.citrinscore.ui.viewer

import androidx.compose.ui.geometry.Offset
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
}
