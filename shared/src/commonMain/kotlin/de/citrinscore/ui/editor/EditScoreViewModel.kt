package de.citrinscore.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.PageEdit
import de.citrinscore.score.PageCrop
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.render.ScoreRenderer
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.openImageSource
import de.citrinscore.ui.components.nextStep
import de.citrinscore.ui.components.previousStep
import de.citrinscore.ui.pages.PreviewPage
import de.citrinscore.ui.pages.previewPage
import de.citrinscore.ui.pages.previewSize
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditScoreViewModel(
    private val folder: PlatformFile,
    private val score: PlatformFile,
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditScoreState())
    val state: StateFlow<EditScoreState> = _state.asStateFlow()

    private var renderer: ScoreRenderer? = null

    init {
        viewModelScope.launch {
            val loaded = attempt {
                val manifest = repository.load(score)
                val titles = repository.content(folder).scoreTitles.filterNot { it.equals(manifest.metadata.title, ignoreCase = true) }
                manifest to titles
            } ?: return@launch _state.update { it.copy(loadFailed = true) }
            val (manifest, titles) = loaded
            val opened = ScoreRenderers.open(score, manifest.content)
            renderer = opened
            _state.update {
                it.copy(
                    isLoaded = true,
                    canAddPages = manifest.content is ScoreContent.Images,
                    pages = opened.pageSizes.indices.map(EditablePage::Existing),
                    title = manifest.metadata.title,
                    composer = manifest.metadata.composer,
                    description = manifest.metadata.description,
                    takenTitles = titles,
                )
            }
            opened.pageSizes.indices.forEach { index -> loadPreview(EditablePage.Existing(index)) { opened.existingPreview(index) } }
        }
    }

    fun addImages(files: List<PlatformFile>) {
        val known = _state.value.pages.map { it.key }.toSet()
        val added = files.map(EditablePage::Added).distinctBy { it.key }.filter { it.key !in known }
        _state.update { it.copy(pages = it.pages + added) }
        added.forEach { page -> loadPreview(page) { openImageSource(page.file).use { it.previewPage(0) } } }
    }

    fun movePage(from: Int, to: Int) = _state.update { state ->
        state.copy(pages = state.pages.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun removePage(index: Int) = _state.update { state ->
        val key = state.pages[index].key
        state.copy(pages = state.pages.filterIndexed { i, _ -> i != index }, failedPreviews = state.failedPreviews - key)
    }

    fun updateTitle(title: String) = _state.update { it.copy(title = title) }

    fun updateComposer(composer: String) = _state.update { it.copy(composer = composer) }

    fun updateDescription(description: String) = _state.update { it.copy(description = description) }

    fun back() = _state.update { it.copy(step = it.step.previousStep()) }

    fun next(onSaved: (PlatformFile) -> Unit) {
        val current = _state.value
        if (!current.canAdvance) return
        when (current.step) {
            EditStep.Details -> save(current, onSaved)
            else -> _state.update { it.copy(step = it.step.nextStep()) }
        }
    }

    private fun loadPreview(page: EditablePage, render: suspend () -> PreviewPage) {
        viewModelScope.launch {
            val preview = attempt { render() }
            _state.update { state ->
                if (preview == null) state.copy(failedPreviews = state.failedPreviews + page.key)
                else state.copy(previews = state.previews + (page.key to preview))
            }
        }
    }

    private suspend fun ScoreRenderer.existingPreview(index: Int): PreviewPage {
        val size = pageSizes[index]
        val image = render(index, Rect(Offset.Zero, size), previewSize(size.width, size.height))
        return PreviewPage(image, PageCrop(0f, 0f, size.width, size.height))
    }

    private fun save(current: EditScoreState, onSaved: (PlatformFile) -> Unit) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val metadata = ScoreMetadata(current.title.trim(), current.composer.trim(), current.description.trim())
            val edits = current.pages.map { page ->
                when (page) {
                    is EditablePage.Existing -> PageEdit.Keep(page.index)
                    is EditablePage.Added -> PageEdit.Add(page.file, current.previews.getValue(page.key).crop)
                }
            }
            val saved = attempt { repository.updateScore(folder, score, metadata, edits) }
            if (saved == null) _state.update { it.copy(isSaving = false, saveFailed = true) } else onSaved(saved)
        }
    }

    override fun onCleared() {
        renderer?.close()
    }
}
