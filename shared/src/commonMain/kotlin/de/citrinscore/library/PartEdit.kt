package de.citrinscore.library

import de.citrinscore.score.PageCrop
import de.citrinscore.score.RecognizedMusic
import io.github.vinceglb.filekit.PlatformFile

data class NewPart(val name: String, val source: PartSource, val recognition: RecognizedMusic? = null)

sealed interface PartSource {
    data class Images(val files: List<PlatformFile>, val crops: List<PageCrop>) : PartSource

    data class Scans(val images: List<ByteArray>, val crops: List<PageCrop>) : PartSource

    data class Pdf(val file: PlatformFile, val crops: List<PageCrop>) : PartSource

    data class MusicXml(val file: PlatformFile) : PartSource
}

sealed interface PartEdit {
    data class Keep(
        val directory: String,
        val name: String,
        val pages: List<PageEdit>? = null,
        val recognition: RecognitionEdit = RecognitionEdit.Unchanged,
    ) : PartEdit

    data class Add(val part: NewPart) : PartEdit
}

sealed interface RecognitionEdit {
    data object Unchanged : RecognitionEdit

    data object Remove : RecognitionEdit

    class Replace(val music: RecognizedMusic) : RecognitionEdit
}
