package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile

interface ImageSource : AutoCloseable {
    val size: IntSize

    fun render(region: Rect, size: IntSize): ImageBitmap
}

expect suspend fun ImageSource(file: PlatformFile): ImageSource
