package de.citrinscore.ui.viewer

import androidx.compose.ui.geometry.Size
import de.citrinscore.library.PartRef
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScorePart
import de.citrinscore.score.ScoreViewState

sealed interface ScoreViewerState {
    data object Loading : ScoreViewerState
    data object Failed : ScoreViewerState
    data class Ready(
        val metadata: ScoreMetadata,
        val parts: List<ScorePart>,
        val part: PartRef,
        val pageSizes: List<Size>,
        val view: ScoreViewState?,
        val reflowScale: Float?,
    ) : ScoreViewerState {
        val isReflowable get() = reflowScale != null
    }
}
