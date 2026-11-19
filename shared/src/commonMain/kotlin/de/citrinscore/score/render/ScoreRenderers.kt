package de.citrinscore.score.render

import de.citrinscore.score.CitScore
import de.citrinscore.score.ScoreContent
import io.github.vinceglb.filekit.PlatformFile

object ScoreRenderers {
    suspend fun open(part: PlatformFile, content: ScoreContent): ScoreRenderer = when (content) {
        is ScoreContent.Pages -> SourceScoreRenderer(
            pages = content.pages.map { page -> SourcePage(CitScore.resolve(part, page.shown), page.shownPage, page.crop) },
            open = ::openPageSource,
        )
        is ScoreContent.MusicXml -> PageSourceRenderer(openMusicXmlSource(CitScore.resolve(part, content.path)))
    }
}
