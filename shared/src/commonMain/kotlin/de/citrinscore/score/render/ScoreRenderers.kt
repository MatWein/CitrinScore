package de.citrinscore.score.render

import de.citrinscore.score.CitScore
import de.citrinscore.score.ScoreContent
import io.github.vinceglb.filekit.PlatformFile

object ScoreRenderers {
    fun open(score: PlatformFile, content: ScoreContent): ScoreRenderer = when (content) {
        is ScoreContent.Images -> ImageScoreRenderer(
            files = content.pages.map { CitScore.resolve(score, it.path) },
            crops = content.pages.map { requireNotNull(it.crop) { "${it.path} has no crop" } },
        )
    }
}
