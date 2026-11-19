package de.citrinscore.ui.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.PageEdit
import de.citrinscore.library.PartEdit
import de.citrinscore.library.PartRef
import de.citrinscore.library.RecognitionEdit
import de.citrinscore.score.PageCrop
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.pageCount
import de.citrinscore.score.render.ScoreRenderer
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.openImageSource
import de.citrinscore.score.omr.RecognitionPage
import de.citrinscore.score.omr.musicXmlPreview
import de.citrinscore.ui.importer.PartDraft
import de.citrinscore.ui.importer.PartDraftEditor
import de.citrinscore.ui.importer.OmrState
import de.citrinscore.ui.importer.importType
import de.citrinscore.ui.importer.launchMusicRecognition
import de.citrinscore.ui.importer.recognitionPage
import de.citrinscore.ui.pages.PreviewPage
import de.citrinscore.ui.pages.previewPage
import de.citrinscore.ui.pages.previewSize
import de.citrinscore.util.attempt
import de.citrinscore.util.logChanges
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(EditScoreViewModel::class.java)

class EditScoreViewModel(
    private val folder: PlatformFile,
    private val score: PlatformFile,
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditScoreState())
    val state: StateFlow<EditScoreState> = _state.asStateFlow()

    val editor = PartDraftEditor(viewModelScope)

    private var manifest: ScoreManifest? = null

    init {
        viewModelScope.launchMusicRecognition(
            requests = _state.map { it.omrRequest },
            current = { _state.value.pagesDraft?.omr },
            pages = { input -> recognitionPages(input as EditOmrInput) },
            publish = { input, omr -> _state.update { state -> if (state.omrRequest == input) state.copy(pagesDraft = state.pagesDraft?.copy(omr = omr)) else state } },
        )
        logger.info("Editing score {}", score.path)
        viewModelScope.logChanges(_state.map { it.step }, logger, "Edit step {}")
        viewModelScope.launch {
            val loaded = attempt {
                val manifest = repository.load(score)
                val titles = repository.content(folder).scoreTitles.filterNot { it.equals(manifest.metadata.title, ignoreCase = true) }
                manifest to titles
            } ?: return@launch _state.update { it.copy(loadFailed = true) }
            val (loadedManifest, titles) = loaded
            manifest = loadedManifest
            _state.update {
                it.copy(
                    isLoaded = true,
                    parts = loadedManifest.parts.map { part ->
                        EditablePart.Existing(part.directory, part.name, part.content.importType, part.content.pageCount, storedRecognition = part.recognition != null)
                    },
                    title = loadedManifest.metadata.title,
                    composer = loadedManifest.metadata.composer,
                    description = loadedManifest.metadata.description,
                    takenTitles = titles,
                )
            }
        }
    }

    fun updateTitle(title: String) = _state.update { it.copy(title = title) }

    fun updateComposer(composer: String) = _state.update { it.copy(composer = composer) }

    fun updateDescription(description: String) = _state.update { it.copy(description = description) }

    fun addPart() {
        editor.edit(PartDraft())
        _state.update { it.copy(step = EditStep.Type, editingPart = null) }
    }

    fun editPart(index: Int) {
        when (val part = _state.value.parts[index]) {
            is EditablePart.New -> {
                editor.edit(part.draft)
                _state.update { it.copy(step = EditStep.Files, editingPart = index) }
            }
            is EditablePart.Existing -> {
                val original = (0 until (part.pageCount ?: 0)).map(EditablePage::Existing)
                val draft = PagesDraft(
                    directory = part.directory,
                    name = part.name,
                    type = part.type,
                    pages = part.pages ?: original,
                    originalPages = original,
                    storedRecognition = part.storedRecognition,
                    omrEnabled = part.omrEnabled,
                    omr = part.omr,
                )
                _state.update { it.copy(step = EditStep.Pages, editingPart = index, pagesDraft = draft) }
                if (part.pageCount != null) loadExistingPreviews(part.directory)
                if (part.storedRecognition) loadStoredRecognition(part.directory)
            }
        }
    }

    fun movePart(from: Int, to: Int) = _state.update { state ->
        state.copy(parts = state.parts.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun removePart(index: Int) {
        (_state.value.parts[index] as? EditablePart.New)?.let { editor.discard(it.draft) }
        _state.update { state -> state.copy(parts = state.parts.filterIndexed { i, _ -> i != index }) }
    }

    fun updatePartName(name: String) = updatePages { it.copy(name = name) }

    fun addImages(files: List<PlatformFile>) {
        val draft = _state.value.pagesDraft ?: return
        val known = draft.pages.map { it.key }.toSet()
        val added = files.map(EditablePage::Added).distinctBy { it.key }.filter { it.key !in known }
        logger.info("Adding images {} to part {}", added.map { it.file.path }, draft.directory)
        updatePages { it.copy(pages = it.pages + added) }
        added.forEach { page -> loadPreview(previewKey(draft.directory, page)) { openImageSource(page.file).use { it.previewPage(0) } } }
    }

    fun enableOmr(enabled: Boolean) = updatePages { it.copy(omrEnabled = enabled, omr = it.omr as? OmrState.Done) }

    fun movePage(from: Int, to: Int) = updatePages { draft ->
        draft.copy(pages = draft.pages.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun removePage(index: Int) = updatePages { draft -> draft.copy(pages = draft.pages.filterIndexed { i, _ -> i != index }) }

    fun back() = _state.update { state ->
        when (state.step) {
            EditStep.Details -> state
            EditStep.Type, EditStep.Pages, EditStep.Parts -> state.copy(
                step = if (state.step == EditStep.Parts) EditStep.Details else EditStep.Parts,
                pagesDraft = null,
                editingPart = null,
            )
            EditStep.Files -> state.copy(step = if (state.editingPart == null) EditStep.Type else EditStep.Parts)
        }
    }

    fun next(onSaved: (PlatformFile) -> Unit) {
        val current = _state.value
        val draft = editor.draft.value
        if (!current.canAdvance(draft)) return
        when (current.step) {
            EditStep.Details -> _state.update { it.copy(step = EditStep.Parts) }
            EditStep.Type -> _state.update { it.copy(step = EditStep.Files) }
            EditStep.Files -> commit(EditablePart.New(draft))
            EditStep.Pages -> {
                val pages = current.pagesDraft ?: return
                val part = current.editingPart?.let { current.parts[it] } as? EditablePart.Existing ?: return
                commit(part.copy(name = pages.name, pages = pages.pages.takeIf { pages.canEditPages }, omrEnabled = pages.omrEnabled, omr = current.recognizedPages))
            }
            EditStep.Parts -> save(current, onSaved)
        }
    }

    private fun commit(part: EditablePart) = _state.update { state ->
        val parts = state.editingPart?.let { index -> state.parts.toMutableList().apply { set(index, part) } } ?: (state.parts + part)
        state.copy(step = EditStep.Parts, parts = parts, editingPart = null, pagesDraft = null)
    }

    private fun updatePages(transform: (PagesDraft) -> PagesDraft) = _state.update { state ->
        state.copy(pagesDraft = state.pagesDraft?.let(transform))
    }

    private fun loadExistingPreviews(directory: String) {
        val part = manifest?.part(directory) ?: return
        val state = _state.value
        val missing = (0 until (part.content.pageCount ?: 0)).filter { index ->
            val key = previewKey(directory, EditablePage.Existing(index))
            key !in state.previews && key !in state.failedPreviews
        }
        if (missing.isEmpty()) return
        viewModelScope.launch {
            val renderer = attempt { ScoreRenderers.open(PartRef(score, directory).root, part.content) }
            if (renderer == null) {
                _state.update { it.copy(failedPreviews = it.failedPreviews + missing.map { index -> previewKey(directory, EditablePage.Existing(index)) }) }
                return@launch
            }
            renderer.use {
                missing.forEach { index ->
                    publishPreview(previewKey(directory, EditablePage.Existing(index)), attempt { renderer.existingPreview(index) })
                }
            }
        }
    }

    private fun loadStoredRecognition(directory: String) {
        val recognition = manifest?.part(directory)?.recognition ?: return
        viewModelScope.launch {
            val preview = attempt { repository.recognition(PartRef(score, directory), recognition)?.let { musicXmlPreview(it.musicXml) } } ?: return@launch
            updatePages { draft -> if (draft.directory == directory) draft.copy(storedPreview = preview) else draft }
        }
    }

    private suspend fun recognitionPages(input: EditOmrInput): List<RecognitionPage> {
        val part = manifest?.part(input.directory) ?: error("Unknown part ${input.directory}")
        return ScoreRenderers.open(PartRef(score, input.directory).root, part.content).use { renderer ->
            input.pages.zip(input.crops).map { (page, crop) ->
                when (page) {
                    is EditablePage.Existing -> renderer.recognitionPage(page.index)
                    is EditablePage.Added -> openImageSource(page.file).use { it.recognitionPage(0, crop.rect) }
                }
            }
        }
    }

    private fun loadPreview(key: String, render: suspend () -> PreviewPage) {
        viewModelScope.launch { publishPreview(key, attempt { render() }) }
    }

    private fun publishPreview(key: String, preview: PreviewPage?) = _state.update { state ->
        if (preview == null) state.copy(failedPreviews = state.failedPreviews + key) else state.copy(previews = state.previews + (key to preview))
    }

    private suspend fun ScoreRenderer.existingPreview(index: Int): PreviewPage {
        val size = pageSizes[index]
        val image = render(index, Rect(Offset.Zero, size), previewSize(size.width, size.height))
        return PreviewPage(image, PageCrop(0f, 0f, size.width, size.height))
    }

    private fun save(current: EditScoreState, onSaved: (PlatformFile) -> Unit) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = attempt {
                val metadata = ScoreMetadata(current.title.trim(), current.composer.trim(), current.description.trim())
                val edits = current.parts.flatMap { part -> part.edits(current) }
                repository.updateScore(folder, score, metadata, edits)
            }
            if (saved == null) _state.update { it.copy(isSaving = false, saveFailed = true) } else onSaved(saved)
        }
    }

    private suspend fun EditablePart.edits(current: EditScoreState): List<PartEdit> = when (this) {
        is EditablePart.Existing -> listOf(PartEdit.Keep(
            directory = directory,
            name = name.trim(),
            recognition = when {
                omrEnabled && omr != null -> RecognitionEdit.Replace(omr.music)
                omrEnabled -> RecognitionEdit.Unchanged
                storedRecognition -> RecognitionEdit.Remove
                else -> RecognitionEdit.Unchanged
            },
            pages = pages?.map { page ->
                when (page) {
                    is EditablePage.Existing -> PageEdit.Keep(page.index)
                    is EditablePage.Added -> PageEdit.Add(page.file, current.previews.getValue(previewKey(directory, page)).crop)
                }
            },
        ))
        is EditablePart.New -> listOf(PartEdit.Add(editor.newPart(draft)))
    }
}
