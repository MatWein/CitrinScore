package de.citrinscore.library

import de.citrinscore.score.CitScore
import de.citrinscore.score.PartDisplay
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.path

data class PartRef(val score: PlatformFile, val directory: String) {
    val root get() = CitScore.resolve(score, directory)

    override fun toString() = "${score.path}/$directory"
}

data class DisplayRef(val part: PartRef, val display: PartDisplay) {
    val annotations get() = part.root / CitScore.annotationsFile(display)

    override fun toString() = "$part as $display"
}
