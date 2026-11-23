package de.citrinscore.ui.viewer

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import de.citrinscore.score.ScoreAnnotations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ScoreDocumentTest {

    private val pageSizes = List(3) { Size(1000f, 1400f) }

    private fun runDocumentTest(block: androidx.compose.ui.test.ComposeUiTest.(ScoreViewport) -> Unit) = runComposeUiTest {
        val viewport = ScoreViewport(null)
        setContent {
            ScoreDocument(
                pageSizes = pageSizes,
                viewport = viewport,
                render = { _, _, size -> ImageBitmap(size.width, size.height) },
                paperColors = null,
                annotations = ScoreAnnotations(),
                annotationInput = null,
                modifier = Modifier.size(400.dp, 600.dp).testTag(TAG),
            )
        }
        waitForIdle()
        block(viewport)
    }

    @Test
    fun `pinching with two fingers zooms around the fingers`() = runDocumentTest { viewport ->
        onNodeWithTag(TAG).performTouchInput {
            pinch(center - Offset(20f, 0f), center - Offset(120f, 0f), center + Offset(20f, 0f), center + Offset(120f, 0f))
        }
        waitForIdle()

        assertTrue(viewport.zoom > 2f, "zoom was ${viewport.zoom}")
        assertTrue(viewport.offset.x > 0f && viewport.offset.y > 0f, "offset was ${viewport.offset}")
    }

    @Test
    fun `dragging with one finger scrolls`() = runDocumentTest { viewport ->
        onNodeWithTag(TAG).performTouchInput { swipeUp() }
        waitForIdle()

        assertEquals(1f, viewport.zoom)
        assertTrue(viewport.offset.y > 0f, "offset was ${viewport.offset}")
    }

    @Test
    fun `dragging with the mouse scrolls`() = runDocumentTest { viewport ->
        onNodeWithTag(TAG).performMouseInput {
            moveTo(center)
            press()
            moveBy(Offset(-100f, -200f))
            release()
        }
        waitForIdle()

        assertEquals(1f, viewport.zoom)
        assertEquals(Offset(0f, 200f), viewport.offset)
    }

    @Test
    fun `a touch drag scrolls by the finger distance only once`() = runDocumentTest { viewport ->
        onNodeWithTag(TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, -100f))
            moveBy(Offset(0f, -100f))
        }
        waitForIdle()

        assertTrue(viewport.offset.y in 150f..200f, "offset was ${viewport.offset}")
    }

    @Test
    fun `mouse wheel scrolls`() = runDocumentTest { viewport ->
        onNodeWithTag(TAG).performMouseInput { scroll(3f) }
        waitForIdle()

        assertEquals(1f, viewport.zoom)
        assertTrue(viewport.offset.y > 0f, "offset was ${viewport.offset}")
    }

    private companion object {
        const val TAG = "document"
    }
}
