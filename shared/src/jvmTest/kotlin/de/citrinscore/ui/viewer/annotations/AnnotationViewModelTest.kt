package de.citrinscore.ui.viewer.annotations

import de.citrinscore.library.LibraryRepository
import de.citrinscore.score.InkStroke
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.TextNote
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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
        val viewModel = AnnotationViewModel(PlatformFile(score), repository)

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
        val viewModel = AnnotationViewModel(PlatformFile(score), repository)
        viewModel.startText(TextDraft(page = 0, x = 0f, y = 0f, size = 1f, original = null))
        viewModel.commitText("mf")
        val note = viewModel.state.value.annotations.texts.single()

        viewModel.startText(TextDraft(note.page, note.x, note.y, note.size, original = note))
        viewModel.commitText("")

        assertSaved(ScoreAnnotations())
    }

    private suspend fun assertSaved(expected: ScoreAnnotations) = withContext(Dispatchers.Default) {
        withTimeout(SAVE_TIMEOUT) { while (repository.annotations(PlatformFile(score)) != expected) delay(POLL_INTERVAL) }
    }
}
