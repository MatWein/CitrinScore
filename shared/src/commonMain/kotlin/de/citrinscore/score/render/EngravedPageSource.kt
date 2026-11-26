package de.citrinscore.score.render

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.CitScore
import de.citrinscore.score.musicxml.EngravedPage
import de.citrinscore.score.musicxml.EngravingLayout
import de.citrinscore.score.musicxml.MusicXmlEngraver
import de.citrinscore.score.musicxml.openMusicXmlEngraver
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.readBytes

class EngravedPageSource(private val pages: List<PageSource>) : PageSource {
    override val pageSizes = pages.map { it.pageSizes.single() }

    override fun render(page: Int, region: Rect, size: IntSize): ImageBitmap = pages[page].render(0, region, size)

    override fun close() = pages.forEach(PageSource::close)
}

suspend fun List<EngravedPage>.pageSource(): PageSource {
    val pages = mutableListOf<PageSource>()
    try {
        forEach { page -> pages += openSvgSource(page.svg.encodeToByteArray()) }
    } catch (e: Exception) {
        pages.forEach(PageSource::close)
        throw e
    }
    return EngravedPageSource(pages)
}

suspend fun openMusicXmlEngraver(file: PlatformFile, firstPartOnly: Boolean = false): MusicXmlEngraver =
    openMusicXmlEngraver(file.readBytes(), file.extension.equals(CitScore.COMPRESSED_MUSICXML_EXTENSION, ignoreCase = true), firstPartOnly)

suspend fun openMusicXmlSource(file: PlatformFile): PageSource = openMusicXmlEngraver(file).use { it.engrave(EngravingLayout.Print) }.pageSource()
