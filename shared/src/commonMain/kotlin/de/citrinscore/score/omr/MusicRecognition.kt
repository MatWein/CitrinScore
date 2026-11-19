package de.citrinscore.score.omr

import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.score.musicxml.EngravingLayout
import de.citrinscore.score.musicxml.openMusicXmlEngraver
import de.citrinscore.score.render.pageSource
import de.citrinscore.ui.pages.PreviewPage
import de.citrinscore.ui.pages.previewPage

expect suspend fun recognizeMusicXml(pages: List<ImageBitmap>, onProgress: (Float) -> Unit): String

suspend fun musicXmlPreview(musicXml: String): List<PreviewPage> =
    openMusicXmlEngraver(musicXml.encodeToByteArray(), compressed = false).use { it.engrave(EngravingLayout.Print) }.pageSource().use { source ->
        source.pageSizes.indices.map { source.previewPage(it) }
    }
