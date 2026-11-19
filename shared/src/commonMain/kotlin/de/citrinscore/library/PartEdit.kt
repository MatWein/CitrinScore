package de.citrinscore.library

import de.citrinscore.score.PageCrop
import de.citrinscore.score.RecognizedMusic
import de.citrinscore.score.scan.EraserStroke
import io.github.vinceglb.filekit.PlatformFile

data class NewPart(val name: String, val source: PartSource, val recognition: RecognizedMusic? = null)

sealed interface PartSource {
    data class Pages(val pages: List<NewPage>) : PartSource

    data class MusicXml(val file: PlatformFile) : PartSource
}

sealed interface PageOrigin {
    data class File(val file: PlatformFile) : PageOrigin

    data class Stored(val path: String) : PageOrigin
}

class NewPage(
    val origin: PageOrigin,
    val page: Int,
    val crop: PageCrop,
    val normalization: NewNormalization? = null,
    val previousIndex: Int? = null,
)

class NewNormalization(val image: ByteArray, val threshold: Float, val erased: List<EraserStroke>)

sealed interface PartEdit {
    data class Keep(
        val directory: String,
        val name: String,
        val source: PartSource? = null,
        val recognition: RecognitionEdit = RecognitionEdit.Unchanged,
    ) : PartEdit

    data class Add(val part: NewPart) : PartEdit
}

sealed interface RecognitionEdit {
    data object Unchanged : RecognitionEdit

    data object Remove : RecognitionEdit

    class Replace(val music: RecognizedMusic) : RecognitionEdit
}
