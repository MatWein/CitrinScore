package de.citrinscore.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.NewPart
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ImportViewModel(
    private val folder: PlatformFile,
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    val editor = PartDraftEditor(viewModelScope)

    init {
        viewModelScope.launch {
            val titles = repository.content(folder).scoreTitles
            _state.update { it.copy(takenTitles = titles) }
        }
    }

    fun updateTitle(title: String) = _state.update { it.copy(title = title) }

    fun updateComposer(composer: String) = _state.update { it.copy(composer = composer) }

    fun updateDescription(description: String) = _state.update { it.copy(description = description) }

    fun addPart() {
        editor.edit(PartDraft())
        _state.update { it.copy(step = ImportStep.Type, editingPart = null) }
    }

    fun editPart(index: Int) {
        editor.edit(_state.value.parts[index])
        _state.update { it.copy(step = ImportStep.Files, editingPart = index) }
    }

    fun movePart(from: Int, to: Int) = _state.update { state ->
        state.copy(parts = state.parts.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun removePart(index: Int) {
        editor.discard(_state.value.parts[index])
        _state.update { state -> state.copy(parts = state.parts.filterIndexed { i, _ -> i != index }) }
    }

    fun back() = _state.update { state ->
        when (state.step) {
            ImportStep.Details -> state
            ImportStep.Type -> state.copy(step = if (state.parts.isEmpty()) ImportStep.Details else ImportStep.Parts)
            ImportStep.Files -> state.copy(step = if (state.editingPart == null) ImportStep.Type else ImportStep.Parts)
            ImportStep.Parts -> state.copy(step = ImportStep.Details)
        }
    }

    fun next(onImported: () -> Unit) {
        val current = _state.value
        val draft = editor.draft.value
        if (!current.canAdvance(draft)) return
        when (current.step) {
            ImportStep.Details -> if (current.parts.isEmpty()) addPart() else _state.update { it.copy(step = ImportStep.Parts) }
            ImportStep.Type -> _state.update { it.copy(step = ImportStep.Files) }
            ImportStep.Files -> _state.update { state ->
                val parts = state.editingPart?.let { index -> state.parts.toMutableList().apply { set(index, draft) } } ?: (state.parts + draft)
                state.copy(step = ImportStep.Parts, parts = parts, editingPart = null)
            }
            ImportStep.Parts -> save(current, onImported)
        }
    }

    private fun save(current: ImportState, onImported: () -> Unit) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = attempt {
                val metadata = ScoreMetadata(current.title.trim(), current.composer.trim(), current.description.trim())
                val parts = current.parts.map { NewPart(it.name.trim(), editor.source(it)) }
                repository.importScore(folder, metadata, parts)
            }
            if (saved == null) _state.update { it.copy(isSaving = false, saveFailed = true) } else onImported()
        }
    }
}
