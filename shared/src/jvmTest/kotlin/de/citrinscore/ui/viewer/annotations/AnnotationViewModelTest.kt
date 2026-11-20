package de.citrinscore.ui.viewer.annotations

import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.DisplayRef
import de.citrinscore.library.PartRef
import de.citrinscore.score.PartDisplay
import androidx.compose.ui.geometry.Rect
import de.citrinscore.score.InkStroke
import de.citrinscore.score.MeasureAnchor
import de.citrinscore.score.MeasureStroke
import de.citrinscore.score.musicxml.MeasureAnchors
import de.citrinscore.score.musicxml.StaffBox
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.TextNote
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val SAVE_TIMEOUT = 5.seconds
private val POLL_INTERVAL = 10.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AnnotationViewModelTest {

    private val score = Files.createTempDirectory("citrinscore-annotations").toFile()
    private val repository = LibraryRepository()
    private val part = DisplayRef(PartRef(PlatformFile(score), ""), PartDisplay.Original)
    private val stroke = InkStroke(page = 0, points = listOf(1f, 1f, 5f, 5f), width = 1f)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun cleanUp() {
        Dispatchers.resetMain()
        score.deleteRecursively()
    }

    @Test
    fun `strokes, texts and erasing are saved and can be undone`() = runTest {
        val viewModel = AnnotationViewModel(MutableStateFlow(part), repository, MutableStateFlow(null))

        viewModel.addStrokes(listOf(stroke))
        viewModel.startText(TextDraft(page = 1, x = 2f, y = 3f, size = 4f, original = null))
        viewModel.commitText("  ff ")
        val text = TextNote(page = 1, x = 2f, y = 3f, text = "ff", size = 4f)
        assertSaved(ScoreAnnotations(listOf(stroke), listOf(text)))

        viewModel.erase(setOf(stroke), emptySet())
        assertSaved(ScoreAnnotations(texts = listOf(text)))

        viewModel.undo()
        viewModel.undo()
        assertSaved(ScoreAnnotations(listOf(stroke)))
        viewModel.undo()
        assertEquals(ScoreAnnotations(), viewModel.state.value.annotations)
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `clearing the text of a note removes it`() = runTest {
        val viewModel = AnnotationViewModel(MutableStateFlow(part), repository, MutableStateFlow(null))
        viewModel.startText(TextDraft(page = 0, x = 0f, y = 0f, size = 1f, original = null))
        viewModel.commitText("mf")
        val note = viewModel.state.value.annotations.texts.single()

        viewModel.startText(TextDraft(note.page, note.x, note.y, note.size, original = note))
        viewModel.commitText("")

        assertSaved(ScoreAnnotations())
    }

    @Test
    fun `switching the part shows its annotations and starts a new history`() = runTest {
        val other = DisplayRef(PartRef(PlatformFile(score), "parts/other"), PartDisplay.Original)
        File(score, other.part.directory).mkdirs()
        repository.saveAnnotations(other, ScoreAnnotations(strokes = listOf(stroke)))
        val selected = MutableStateFlow(part)
        val viewModel = AnnotationViewModel(selected, repository, MutableStateFlow(null))
        viewModel.addStrokes(listOf(stroke.copy(page = 1)))

        selected.value = other

        withContext(Dispatchers.Default) {
            withTimeout(SAVE_TIMEOUT) { while (viewModel.state.value.annotations.strokes != listOf(stroke)) delay(POLL_INTERVAL) }
        }
        assertFalse(viewModel.state.value.canUndo)
    }

    @Test
    fun `every display of a part keeps its own annotations`() = runTest {
        val normalized = part.copy(display = PartDisplay.Normalized)
        val selected = MutableStateFlow(part)
        val viewModel = AnnotationViewModel(selected, repository, MutableStateFlow(null))
        viewModel.addStrokes(listOf(stroke))
        assertSaved(ScoreAnnotations(listOf(stroke)))

        selected.value = normalized

        withContext(Dispatchers.Default) {
            withTimeout(SAVE_TIMEOUT) { while (viewModel.state.value.annotations.strokes.isNotEmpty()) delay(POLL_INTERVAL) }
        }
        assertEquals(ScoreAnnotations(), repository.annotations(normalized))
        assertEquals(ScoreAnnotations(listOf(stroke)), repository.annotations(part))
    }

    @Test
    fun `strokes on reflowed parts are stored per measure and follow the layout`() = runTest {
        val layout = MutableStateFlow<MeasureAnchors?>(anchors(left = 0f))
        val viewModel = AnnotationViewModel(MutableStateFlow(part), repository, layout)

        viewModel.addStrokes(listOf(InkStroke(page = 0, points = listOf(50f, 10f), width = 1f)))
        layout.value = anchors(left = 100f)

        assertEquals(listOf(InkStroke(page = 0, points = listOf(150f, 10f), width = 1f)), viewModel.state.value.annotations.strokes)
        val saved = withContext(Dispatchers.Default) {
            withTimeout(SAVE_TIMEOUT) {
                var annotations = repository.annotations(part)
                while (annotations.measureStrokes.isEmpty()) {
                    delay(POLL_INTERVAL)
                    annotations = repository.annotations(part)
                }
                annotations
            }
        }
        assertEquals(listOf(MeasureStroke(MeasureAnchor(0, 0), listOf(0.5f, 1f), 0.1f)), saved.measureStrokes)
        assertEquals(emptyList(), saved.strokes)
    }

    private fun anchors(left: Float) = MeasureAnchors(listOf(listOf(StaffBox(0, 0, Rect(left, 0f, left + 100f, 40f), 10f))))

    private suspend fun assertSaved(expected: ScoreAnnotations) = withContext(Dispatchers.Default) {
        withTimeout(SAVE_TIMEOUT) { while (repository.annotations(part) != expected) delay(POLL_INTERVAL) }
    }
}
