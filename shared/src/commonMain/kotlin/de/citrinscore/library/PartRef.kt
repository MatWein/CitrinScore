package de.citrinscore.library

import de.citrinscore.score.CitScore
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

data class PartRef(val score: PlatformFile, val directory: String) {
    val root get() = CitScore.resolve(score, directory)

    override fun toString() = "${score.path}/$directory"
}
