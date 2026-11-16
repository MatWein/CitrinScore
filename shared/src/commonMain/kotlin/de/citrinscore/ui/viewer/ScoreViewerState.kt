package de.citrinscore.ui.viewer

import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScoreViewState

sealed interface ScoreViewerState {
    data object Loading : ScoreViewerState
    data object Failed : ScoreViewerState
    data class Ready(val metadata: ScoreMetadata, val pageSizes: List<IntSize>, val view: ScoreViewState?) : ScoreViewerState
}
