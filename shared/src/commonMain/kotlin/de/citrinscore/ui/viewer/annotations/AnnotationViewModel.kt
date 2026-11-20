package de.citrinscore.ui.viewer.annotations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.DisplayRef
import de.citrinscore.score.InkStroke
import de.citrinscore.score.MeasureStroke
import de.citrinscore.score.MeasureText
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.TextNote
import de.citrinscore.score.musicxml.MeasureAnchors
import de.citrinscore.util.attempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AnnotationViewModel(
    private val display: StateFlow<DisplayRef?>,
    private val repository: LibraryRepository,
    private val anchors: StateFlow<MeasureAnchors?>,
) : ViewModel() {

    private val _state = MutableStateFlow(AnnotationState())
    val state: StateFlow<AnnotationState> = _state.asStateFlow()

    private var stored = ScoreAnnotations()
    private var projectedStrokes = emptyMap<InkStroke, MeasureStroke>()
    private var projectedTexts = emptyMap<TextNote, MeasureText>()
    private val history = ArrayDeque<ScoreAnnotations>()
    private val saving = Mutex()

    init {
        viewModelScope.launch {
            display.filterNotNull().collectLatest { ref ->
                history.clear()
                show(ScoreAnnotations())
                _state.update { it.copy(draft = null, canUndo = false) }
                attempt { repository.annotations(ref) }?.let(::show)
            }
        }
        viewModelScope.launch { anchors.collect { show(stored) } }
    }

    fun toggleEditing() = _state.update { it.copy(isEditing = !it.isEditing, draft = null) }

    fun selectTool(tool: AnnotationTool) = _state.update { it.copy(tool = tool) }

    fun addStrokes(strokes: List<InkStroke>) = change { annotations ->
        anchors.value?.let { layout -> annotations.copy(measureStrokes = annotations.measureStrokes + strokes.flatMap(layout::anchor)) }
            ?: annotations.copy(strokes = annotations.strokes + strokes)
    }

    fun erase(strokes: Set<InkStroke>, texts: Set<TextNote>) {
        if (strokes.isEmpty() && texts.isEmpty()) return
        change { annotations ->
            annotations.copy(
                strokes = annotations.strokes - strokes,
                texts = annotations.texts - texts,
                measureStrokes = annotations.measureStrokes - strokes.mapNotNull(projectedStrokes::get).toSet(),
                measureTexts = annotations.measureTexts - texts.mapNotNull(projectedTexts::get).toSet(),
            )
        }
    }

    fun startText(draft: TextDraft) = _state.update { it.copy(draft = draft) }

    fun commitText(text: String) {
        val draft = _state.value.draft ?: return
        _state.update { it.copy(draft = null) }
        val trimmed = text.trim()
        val note = draft.original?.copy(text = trimmed) ?: TextNote(draft.page, draft.x, draft.y, trimmed, draft.size)
        change { annotations ->
            val layout = anchors.value
            if (layout == null) {
                val remaining = annotations.texts - setOfNotNull(draft.original)
                annotations.copy(texts = if (trimmed.isEmpty()) remaining else remaining + note)
            } else {
                val original = draft.original?.let(projectedTexts::get)
                val remaining = annotations.measureTexts - setOfNotNull(original)
                val anchored = original?.copy(text = trimmed) ?: layout.anchor(note)
                annotations.copy(measureTexts = if (trimmed.isEmpty() || anchored == null) remaining else remaining + anchored)
            }
        }
    }

    fun cancelText() = _state.update { it.copy(draft = null) }

    fun undo() {
        val previous = history.removeLastOrNull() ?: return
        show(previous)
        _state.update { it.copy(canUndo = history.isNotEmpty()) }
        save(previous)
    }

    fun dismissSaveFailure() = _state.update { it.copy(saveFailed = false) }

    private fun show(annotations: ScoreAnnotations) {
        stored = annotations
        val layout = anchors.value
        projectedStrokes = layout?.let { annotations.measureStrokes.mapNotNull { stroke -> layout.project(stroke)?.let { it to stroke } }.toMap() }.orEmpty()
        projectedTexts = layout?.let { annotations.measureTexts.mapNotNull { text -> layout.project(text)?.let { it to text } }.toMap() }.orEmpty()
        val visible = if (layout == null) ScoreAnnotations(annotations.strokes, annotations.texts) else ScoreAnnotations(projectedStrokes.keys.toList(), projectedTexts.keys.toList())
        _state.update { it.copy(annotations = visible) }
    }

    private fun change(transform: (ScoreAnnotations) -> ScoreAnnotations) {
        val current = stored
        val changed = transform(current)
        if (changed == current) return
        history.addLast(current)
        show(changed)
        _state.update { it.copy(canUndo = true) }
        save(changed)
    }

    private fun save(annotations: ScoreAnnotations) {
        val ref = display.value ?: return
        viewModelScope.launch {
            saving.withLock {
                attempt { repository.saveAnnotations(ref, annotations) } ?: _state.update { it.copy(saveFailed = true) }
            }
        }
    }
}
