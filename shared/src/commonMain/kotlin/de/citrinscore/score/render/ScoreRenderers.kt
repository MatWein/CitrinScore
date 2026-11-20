package de.citrinscore.score.render

import de.citrinscore.score.CitScore
import de.citrinscore.score.PartDisplay
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScorePage
import de.citrinscore.score.scan.SCAN_PAGE_WIDTH
import io.github.vinceglb.filekit.PlatformFile

object ScoreRenderers {
    suspend fun open(part: PlatformFile, content: ScoreContent, display: PartDisplay): ScoreRenderer = when (content) {
        is ScoreContent.Pages -> SourceScoreRenderer(pages = content.pages.map { it.sourcePage(part, display) }, open = ::openPageSource)
        is ScoreContent.MusicXml -> PageSourceRenderer(openMusicXmlSource(CitScore.resolve(part, content.path)))
    }

    private fun ScorePage.sourcePage(part: PlatformFile, display: PartDisplay) =
        if (display == PartDisplay.Original) {
            SourcePage(CitScore.resolve(part, source), page, crop, unitWidth = SCAN_PAGE_WIDTH)
        } else {
            SourcePage(CitScore.resolve(part, normalization.image), 0, crop)
        }
}
