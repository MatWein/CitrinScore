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
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScoreViewModel(private val score: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow<ScoreViewerState>(ScoreViewerState.Loading)
    val state: StateFlow<ScoreViewerState> = _state.asStateFlow()

    private var renderer: ScoreRenderer? = null
    private var savedView: ScoreViewState? = null

    init {
        viewModelScope.launch {
            _state.value = try {
                val manifest = repository.open(score)
                val opened = ScoreRenderers.open(score, manifest.content)
                renderer = opened
                savedView = manifest.view
                ScoreViewerState.Ready(manifest.metadata, opened.pageSizes, manifest.view)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                ScoreViewerState.Failed
            }
        }
    }

    suspend fun render(page: Int, region: Rect, size: IntSize): ImageBitmap? = try {
        renderer?.render(page, region, size)
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }

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

    override fun onCleared() {
        renderer?.close()
    }
}
