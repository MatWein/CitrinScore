package de.citrinscore.score.omr

import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.score.RecognizedMusic
import de.citrinscore.score.musicxml.EngravingLayout
import de.citrinscore.score.musicxml.openMusicXmlEngraver
import de.citrinscore.score.render.pageSource
import de.citrinscore.ui.pages.PreviewPage
import de.citrinscore.ui.pages.previewPage

private const val ANALYSIS_SHARE = 0.5f

class RecognitionPage(val image: ImageBitmap, val pixelsPerUnit: Float)

sealed interface RecognitionProgress {
    val fraction: Float?

    data object Preparing : RecognitionProgress {
        override val fraction = null
    }

    data class AnalyzingPage(val page: Int, val pages: Int, val patch: Int, val patches: Int) : RecognitionProgress {
        override val fraction get() = (page + ANALYSIS_SHARE * patch / patches) / pages
    }

    data class ReadingStaff(val page: Int, val pages: Int, val staff: Int, val staffs: Int) : RecognitionProgress {
        override val fraction get() = (page + ANALYSIS_SHARE + (1 - ANALYSIS_SHARE) * staff / staffs) / pages
    }
}

expect suspend fun recognizeMusic(pages: List<RecognitionPage>, onProgress: (RecognitionProgress) -> Unit): RecognizedMusic

suspend fun musicXmlPreview(musicXml: String): List<PreviewPage> =
    openMusicXmlEngraver(musicXml.encodeToByteArray(), compressed = false).use { it.engrave(EngravingLayout.Print) }.pageSource().use { source ->
        source.pageSizes.indices.map { source.previewPage(it) }
    }
