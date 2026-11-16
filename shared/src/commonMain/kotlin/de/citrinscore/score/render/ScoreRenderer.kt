package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

interface ScoreRenderer : AutoCloseable {
    val pageSizes: List<IntSize>

    suspend fun render(page: Int, region: Rect, size: IntSize): ImageBitmap
}
