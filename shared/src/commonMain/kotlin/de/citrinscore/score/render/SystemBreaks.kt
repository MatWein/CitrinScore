package de.citrinscore.score.render

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val MIN_GAP_FRACTION = 0.012f

suspend fun ScoreRenderer.systemBreaks(page: Int): List<Float> {
    val size = pageSizes[page]
    val scale = SCAN_WIDTH / size.width
    val image = render(page, Rect(Offset.Zero, size), scanSize(size, scale))
    return withContext(Dispatchers.Default) {
        InkScan(image).gapCenters(minGap = (SCAN_WIDTH * MIN_GAP_FRACTION).roundToInt()).map { it / scale }
    }
}

internal fun InkScan.gapCenters(minGap: Int): List<Float> = inkRows.zipWithNext()
    .filter { (above, below) -> below - above - 1 >= minGap && !isBridged(above + 1, below) }
    .map { (above, below) -> (above + 1 + below) / 2f }
