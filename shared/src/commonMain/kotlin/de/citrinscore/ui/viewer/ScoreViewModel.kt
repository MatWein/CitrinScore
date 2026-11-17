package de.citrinscore.ui.viewer

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.PartRef
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.render.ScoreRenderer
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.systemBreaks
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScoreViewModel(private val score: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow<ScoreViewerState>(ScoreViewerState.Loading)
    val state: StateFlow<ScoreViewerState> = _state.asStateFlow()

    private val _systemBreaks = MutableStateFlow(emptyList<List<Float>>())
    val systemBreaks: StateFlow<List<List<Float>>> = _systemBreaks.asStateFlow()

    private val _part = MutableStateFlow<PartRef?>(null)
    val part: StateFlow<PartRef?> = _part.asStateFlow()

    private val renderers = mutableMapOf<String, ScoreRenderer>()
    private var manifest: ScoreManifest? = null
    private var detection: Job? = null

    init {
        viewModelScope.launch {
            val opened = attempt { repository.open(score) } ?: return@launch run { _state.value = ScoreViewerState.Failed }
            manifest = opened
            val current = opened.currentPart?.takeIf { directory -> opened.parts.any { it.directory == directory } }
            show(current ?: opened.parts.first().directory)
        }
    }

    fun selectPart(directory: String) {
        if (_part.value?.directory == directory) return
        viewModelScope.launch {
            show(directory)
            attempt { repository.selectPart(PartRef(score, directory)) }
        }
    }

    suspend fun render(part: PartRef, page: Int, region: Rect, size: IntSize): ImageBitmap? =
        attempt { renderers[part.directory]?.render(page, region, size) }

    fun saveView(part: PartRef, view: ScoreViewState?) {
        val current = manifest ?: return
        if (view == null || current.part(part.directory).view == view) return
        manifest = current.updatePart(part.directory) { it.copy(view = view) }
        viewModelScope.launch {
            withContext(NonCancellable) { attempt { repository.saveView(part, view) } }
        }
    }

    private suspend fun show(directory: String) {
        val current = manifest ?: return
        val part = current.part(directory)
        val ref = PartRef(score, directory)
        detection?.cancel()
        val renderer = attempt { renderers.getOrPut(directory) { ScoreRenderers.open(ref.root, part.content) } }
            ?: return run { _state.value = ScoreViewerState.Failed }
        val breaks = part.systemBreaks?.takeIf { it.size == renderer.pageSizes.size }
        _systemBreaks.value = breaks.orEmpty()
        _part.value = ref
        _state.value = ScoreViewerState.Ready(current.metadata, current.parts, ref, renderer.pageSizes, part.view)
        if (breaks == null) detection = viewModelScope.launch { detectSystemBreaks(ref, renderer) }
    }

    private suspend fun detectSystemBreaks(part: PartRef, renderer: ScoreRenderer) {
        for (page in renderer.pageSizes.indices) {
            val breaks = attempt { renderer.systemBreaks(page) } ?: return
            _systemBreaks.update { it + listOf(breaks) }
        }
        val detected = _systemBreaks.value
        manifest = manifest?.updatePart(part.directory) { it.copy(systemBreaks = detected) }
        attempt { repository.saveSystemBreaks(part, detected) }
    }

    override fun onCleared() {
        renderers.values.forEach { it.close() }
    }
}
