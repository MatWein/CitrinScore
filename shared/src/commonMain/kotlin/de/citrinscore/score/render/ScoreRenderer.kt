package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize

interface ScoreRenderer : AutoCloseable {
    val pageSizes: List<Size>

    suspend fun render(page: Int, region: Rect, size: IntSize): ImageBitmap
}
