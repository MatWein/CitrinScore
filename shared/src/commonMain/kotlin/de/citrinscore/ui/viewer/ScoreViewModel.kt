package de.citrinscore.ui.viewer

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.render.ScoreRenderer
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.systemBreaks
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
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

    private var renderer: ScoreRenderer? = null
    private var savedView: ScoreViewState? = null

    init {
        viewModelScope.launch {
            _state.value = attempt {
                val manifest = repository.open(score)
                val opened = ScoreRenderers.open(score, manifest.content)
                renderer = opened
                savedView = manifest.view
                manifest.systemBreaks?.takeIf { it.size == opened.pageSizes.size }?.let { _systemBreaks.value = it }
                ScoreViewerState.Ready(manifest.metadata, opened.pageSizes, manifest.view)
            } ?: ScoreViewerState.Failed
            renderer?.takeIf { _systemBreaks.value.isEmpty() }?.let { detectSystemBreaks(it) }
        }
    }

    private suspend fun detectSystemBreaks(renderer: ScoreRenderer) {
        for (page in renderer.pageSizes.indices) {
            val breaks = attempt { renderer.systemBreaks(page) } ?: return
            _systemBreaks.update { it + listOf(breaks) }
        }
        attempt { repository.saveSystemBreaks(score, _systemBreaks.value) }
    }

    suspend fun render(page: Int, region: Rect, size: IntSize): ImageBitmap? = attempt { renderer?.render(page, region, size) }

    fun saveView(view: ScoreViewState?) {
        if (view == null || view == savedView) return
        savedView = view
        viewModelScope.launch {
            withContext(NonCancellable) {
                try {
                    repository.saveView(score, view)
                } catch (_: Exception) {
                    savedView = null
                }
            }
        }
    }

    private suspend fun <T> attempt(block: suspend () -> T): T? = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

    override fun onCleared() {
        renderer?.close()
    }
}
