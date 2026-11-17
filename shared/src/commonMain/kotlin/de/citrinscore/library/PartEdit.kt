package de.citrinscore.library

import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile

data class NewPart(val name: String, val source: PartSource)

sealed interface PartSource {
    val crops: List<PageCrop>

    data class Images(val files: List<PlatformFile>, override val crops: List<PageCrop>) : PartSource

    data class Scans(val images: List<ByteArray>, override val crops: List<PageCrop>) : PartSource

    data class Pdf(val file: PlatformFile, override val crops: List<PageCrop>) : PartSource
}

sealed interface PartEdit {
    data class Keep(val directory: String, val name: String, val pages: List<PageEdit>? = null) : PartEdit

    data class Add(val part: NewPart) : PartEdit
}
