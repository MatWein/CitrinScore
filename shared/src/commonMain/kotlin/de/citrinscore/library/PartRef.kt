package de.citrinscore.library

import de.citrinscore.score.CitScore
import io.github.vinceglb.filekit.PlatformFile

data class PartRef(val score: PlatformFile, val directory: String) {
    val root get() = CitScore.resolve(score, directory)
}
