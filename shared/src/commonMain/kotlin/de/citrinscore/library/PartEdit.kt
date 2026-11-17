package de.citrinscore.library

import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile

data class NewPart(val name: String, val source: PartSource)

sealed interface PartSource {
    data class Images(val files: List<PlatformFile>, val crops: List<PageCrop>) : PartSource

    data class Scans(val images: List<ByteArray>, val crops: List<PageCrop>) : PartSource

    data class Pdf(val file: PlatformFile, val crops: List<PageCrop>) : PartSource

    data class MusicXml(val file: PlatformFile) : PartSource
}

sealed interface PartEdit {
    data class Keep(val directory: String, val name: String, val pages: List<PageEdit>? = null) : PartEdit

    data class Add(val part: NewPart) : PartEdit
}
