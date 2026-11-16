package de.citrinscore.ui.viewer.annotations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.score.InkStroke
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.TextNote
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AnnotationViewModel(private val score: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(AnnotationState())
    val state: StateFlow<AnnotationState> = _state.asStateFlow()

    private val history = ArrayDeque<ScoreAnnotations>()
    private val saving = Mutex()

    init {
        viewModelScope.launch {
            attempt { repository.annotations(score) }?.let { annotations -> _state.update { it.copy(annotations = annotations) } }
        }
    }

    fun toggleEditing() = _state.update { it.copy(isEditing = !it.isEditing, draft = null) }

    fun selectTool(tool: AnnotationTool) = _state.update { it.copy(tool = tool) }

    fun addStrokes(strokes: List<InkStroke>) = change { it.copy(strokes = it.strokes + strokes) }

    fun erase(strokes: Set<InkStroke>, texts: Set<TextNote>) {
        if (strokes.isEmpty() && texts.isEmpty()) return
        change { it.copy(strokes = it.strokes - strokes, texts = it.texts - texts) }
    }

    fun startText(draft: TextDraft) = _state.update { it.copy(draft = draft) }

    fun commitText(text: String) {
        val draft = _state.value.draft ?: return
        _state.update { it.copy(draft = null) }
        val trimmed = text.trim()
        change { annotations ->
            val remaining = annotations.texts - setOfNotNull(draft.original)
            val note = draft.original?.copy(text = trimmed) ?: TextNote(draft.page, draft.x, draft.y, trimmed, draft.size)
            annotations.copy(texts = if (trimmed.isEmpty()) remaining else remaining + note)
        }
    }

    fun cancelText() = _state.update { it.copy(draft = null) }

    fun undo() {
        val previous = history.removeLastOrNull() ?: return
        _state.update { it.copy(annotations = previous, canUndo = history.isNotEmpty()) }
        save(previous)
    }

    fun dismissSaveFailure() = _state.update { it.copy(saveFailed = false) }

    private fun change(transform: (ScoreAnnotations) -> ScoreAnnotations) {
        val current = _state.value.annotations
        val changed = transform(current)
        if (changed == current) return
        history.addLast(current)
        _state.update { it.copy(annotations = changed, canUndo = true) }
        save(changed)
    }

    private fun save(annotations: ScoreAnnotations) {
        viewModelScope.launch {
            saving.withLock {
                attempt { repository.saveAnnotations(score, annotations) } ?: _state.update { it.copy(saveFailed = true) }
            }
        }
    }
}
