package de.citrinscore.ui.viewer

import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.ScrollMode
import kotlinx.coroutines.test.runTest
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
            scrollMode = ScrollMode.Both
            setBounds(viewportSize, documentSize)
            zoomBy(2f, Offset.Zero)
            scrollBy(Offset(400f, 1500f))
        }

        val restored = ScoreViewport(original.view).apply { setBounds(Size(500f, 400f), Size(500f, 1500f)) }

        assertEquals(ScoreViewState(2f, 0.2f, 0.25f), original.view)
        assertEquals(Offset(200f, 750f), restored.offset)
    }

    @Test
    fun `stepping glides down by half the visible height`() = runTest(FrameClock()) {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize, bottomInset = 100f) }

        viewport.step(1)

        assertEquals(350f, viewport.offset.y, absoluteTolerance = 0.01f)
        assertTrue(viewport.canScrollDown)
    }

    @Test
    fun `stepping stops at the document edges`() = runTest(FrameClock()) {
        val viewport = ScoreViewport(null).apply { setBounds(viewportSize, documentSize) }

        repeat(10) { viewport.step(1) }
        assertEquals(2200f, viewport.offset.y, absoluteTolerance = 0.01f)
        assertFalse(viewport.canScrollDown)

        viewport.step(-1)
        assertEquals(1800f, viewport.offset.y, absoluteTolerance = 0.01f)
    }

    @Test
    fun `the scroll mode limits scrolling to the allowed direction`() {
        val viewport = ScoreViewport(null).apply {
            setBounds(viewportSize, documentSize)
            zoomTo(2f, Offset.Zero)
        }

        viewport.scrollMode = ScrollMode.Vertical
        assertEquals(Offset(0f, 300f), viewport.scrollBy(Offset(200f, 300f)))

        viewport.scrollMode = ScrollMode.Horizontal
        assertEquals(Offset(200f, 0f), viewport.scrollBy(Offset(200f, 300f)))

        viewport.scrollMode = ScrollMode.Both
        assertEquals(Offset(200f, 300f), viewport.scrollBy(Offset(200f, 300f)))
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

private class FrameClock : MonotonicFrameClock {
    private var nanos = 0L

    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R {
        nanos += FRAME_NANOS
        return onFrame(nanos)
    }

    private companion object {
        const val FRAME_NANOS = 16_000_000L
    }
}
