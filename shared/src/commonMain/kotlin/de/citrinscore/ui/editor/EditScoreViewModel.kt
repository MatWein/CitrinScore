package de.citrinscore.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.PartEdit
import de.citrinscore.library.PartRef
import de.citrinscore.library.RecognitionEdit
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.omr.musicXmlPreview
import de.citrinscore.score.pageCount
import de.citrinscore.ui.importer.PartDraft
import de.citrinscore.ui.importer.PartDraftEditor
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
                        EditablePart.Existing(
                            directory = part.directory,
                            name = part.name,
                            musicXml = part.content is ScoreContent.MusicXml,
                            pageCount = part.content.pageCount,
                            playable = part.recognition != null,
                        )
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
        _state.update { it.copy(step = EditStep.Files, editingPart = null) }
    }

    fun editPart(index: Int) {
        when (val part = _state.value.parts[index]) {
            is EditablePart.New -> editor.edit(part.draft)
            is EditablePart.Existing -> part.draft?.let(editor::edit) ?: loadPart(part.directory)
        }
        _state.update { it.copy(step = EditStep.Files, editingPart = index) }
    }

    fun movePart(from: Int, to: Int) = _state.update { state ->
        state.copy(parts = state.parts.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun removePart(index: Int) {
        when (val part = _state.value.parts[index]) {
            is EditablePart.New -> editor.discard(part.draft)
            is EditablePart.Existing -> part.draft?.let(editor::discard)
        }
        _state.update { state -> state.copy(parts = state.parts.filterIndexed { i, _ -> i != index }) }
    }

    fun back() = _state.update { state ->
        when (state.step) {
            EditStep.Details -> state
            EditStep.Files -> state.copy(step = EditStep.Parts, editingPart = null)
            EditStep.Parts -> state.copy(step = EditStep.Details)
        }
    }

    fun next(onSaved: (PlatformFile) -> Unit) {
        val current = _state.value
        val draft = editor.draft.value
        if (!current.canAdvance(draft)) return
        when (current.step) {
            EditStep.Details -> _state.update { it.copy(step = EditStep.Parts) }
            EditStep.Files -> commit(draft)
            EditStep.Parts -> save(current, onSaved)
        }
    }

    private fun loadPart(directory: String) {
        val part = manifest?.part(directory) ?: return
        val ref = PartRef(score, directory)
        editor.load(ref.root, part) {
            part.recognition?.let { recognition -> repository.recognition(ref, recognition)?.let { musicXmlPreview(it.musicXml) } }
        }
    }

    private fun commit(draft: PartDraft) = _state.update { state ->
        val editing = state.editingPart
        val part = when (val previous = editing?.let(state.parts::get)) {
            is EditablePart.Existing -> previous.copy(name = draft.name, draft = draft)
            else -> EditablePart.New(draft)
        }
        val parts = editing?.let { index -> state.parts.toMutableList().apply { set(index, part) } } ?: (state.parts + part)
        state.copy(step = EditStep.Parts, parts = parts, editingPart = null)
    }

    private fun save(current: EditScoreState, onSaved: (PlatformFile) -> Unit) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = attempt {
                val metadata = ScoreMetadata(current.title.trim(), current.composer.trim(), current.description.trim())
                repository.updateScore(folder, score, metadata, current.parts.map { it.edit() })
            }
            if (saved == null) _state.update { it.copy(isSaving = false, saveFailed = true) } else onSaved(saved)
        }
    }

    private suspend fun EditablePart.edit(): PartEdit = when (this) {
        is EditablePart.Existing -> PartEdit.Keep(
            directory = directory,
            name = name.trim(),
            source = draft?.let { editor.source(it) },
            recognition = draft?.recognitionEdit ?: RecognitionEdit.Unchanged,
        )
        is EditablePart.New -> PartEdit.Add(editor.newPart(draft))
    }
}
