package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes

interface PageSource : AutoCloseable {
    val pageSizes: List<Size>

    fun render(page: Int, region: Rect, size: IntSize): ImageBitmap
}

private const val SVG_EXTENSION = "svg"
private const val HALF_TURN_DEGREES = 180
private const val PDF_EXTENSION = "pdf"

suspend fun openPageSource(file: PlatformFile): PageSource =
    if (file.extension.equals(PDF_EXTENSION, ignoreCase = true)) openPdfSource(file) else openImageSource(file)

suspend fun openImageSource(file: PlatformFile): PageSource =
    if (file.extension.equals(SVG_EXTENSION, ignoreCase = true)) openSvgSource(file) else openRasterImageSource(file)

internal fun orientedPageSize(width: Float, height: Float, rotationDegrees: Int) =
    if (rotationDegrees % HALF_TURN_DEGREES == 0) Size(width, height) else Size(height, width)

expect suspend fun openRasterImageSource(file: PlatformFile): PageSource

suspend fun openSvgSource(file: PlatformFile): PageSource = openSvgSource(file.readBytes())

expect suspend fun openSvgSource(bytes: ByteArray): PageSource

expect suspend fun openPdfSource(file: PlatformFile): PageSource
