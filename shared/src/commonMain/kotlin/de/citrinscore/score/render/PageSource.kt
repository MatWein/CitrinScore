package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension

interface PageSource : AutoCloseable {
    val pageSizes: List<Size>

    fun render(page: Int, region: Rect, size: IntSize): ImageBitmap
}

private const val SVG_EXTENSION = "svg"
private const val PDF_EXTENSION = "pdf"

suspend fun openPageSource(file: PlatformFile): PageSource =
    if (file.extension.equals(PDF_EXTENSION, ignoreCase = true)) openPdfSource(file) else openImageSource(file)

suspend fun openImageSource(file: PlatformFile): PageSource =
    if (file.extension.equals(SVG_EXTENSION, ignoreCase = true)) openSvgSource(file) else openRasterImageSource(file)

expect suspend fun openRasterImageSource(file: PlatformFile): PageSource

expect suspend fun openSvgSource(file: PlatformFile): PageSource

expect suspend fun openPdfSource(file: PlatformFile): PageSource
