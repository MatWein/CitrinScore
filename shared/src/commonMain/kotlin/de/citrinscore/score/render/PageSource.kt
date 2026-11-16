package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile

interface PageSource : AutoCloseable {
    val pageSizes: List<Size>

    fun render(page: Int, region: Rect, size: IntSize): ImageBitmap
}

expect suspend fun openImageSource(file: PlatformFile): PageSource

expect suspend fun openPdfSource(file: PlatformFile): PageSource
