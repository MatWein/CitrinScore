package de.citrinscore.score.render

import de.citrinscore.score.CitScore
import de.citrinscore.score.ScoreContent
import io.github.vinceglb.filekit.PlatformFile

object ScoreRenderers {
    suspend fun open(score: PlatformFile, content: ScoreContent): ScoreRenderer = when (content) {
        is ScoreContent.Images -> SourceScoreRenderer(
            pages = content.pages.map { page ->
                SourcePage(CitScore.resolve(score, page.path), 0, requireNotNull(page.crop) { "${page.path} has no crop" })
            },
            open = ::openImageSource,
        )
        is ScoreContent.Pdf -> {
            val document = CitScore.resolve(score, content.path)
            SourceScoreRenderer(content.pages.map { SourcePage(document, it.index, it.crop) }, ::openPdfSource)
        }
        is ScoreContent.MusicXml -> PageSourceRenderer(openMusicXmlSource(CitScore.resolve(score, content.path)))
    }
}
