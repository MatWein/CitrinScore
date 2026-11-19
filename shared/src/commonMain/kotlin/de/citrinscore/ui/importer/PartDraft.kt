package de.citrinscore.ui.importer

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.i18n.Strings
import de.citrinscore.library.RecognitionEdit
import de.citrinscore.score.PageCrop
import de.citrinscore.score.scan.DEFAULT_SCAN_THRESHOLD
import de.citrinscore.score.scan.EraserStroke
import de.citrinscore.ui.pages.PreviewPage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import kotlin.uuid.Uuid

data class DraftPage(
    val file: PlatformFile,
    val page: Int,
    val pageCount: Int,
    val normalize: Boolean,
    val key: String = Uuid.random().toString(),
    val storedPath: String? = null,
    val previousIndex: Int? = null,
    val crop: PageCrop? = null,
    val erased: List<EraserStroke> = emptyList(),
    val sourceSize: Size? = null,
    val image: ImageBitmap? = null,
    val size: Size? = null,
    val failed: Boolean = false,
) {
    val view get() = if (image != null && size != null && crop != null) PageView(image, size, crop, erased) else null

    val label get() = file.name
}

data class DraftMusicXml(val file: PlatformFile, val stored: Boolean = false)

data class StoredRecognition(val input: OmrInput, val preview: List<PreviewPage>? = null)

data class PartDraft(
    val key: String = Uuid.random().toString(),
    val name: String = "",
    val pages: List<DraftPage> = emptyList(),
    val musicXml: DraftMusicXml? = null,
    val threshold: Float = DEFAULT_SCAN_THRESHOLD,
    val erasedPages: List<String> = emptyList(),
    val omrEnabled: Boolean = false,
    val omr: OmrState? = null,
    val storedRecognition: StoredRecognition? = null,
) {
    val isLoadingPreview get() = pages.any { it.view == null && !it.failed }

    val previewFailed get() = pages.any { it.failed }

    val isPreviewReady get() = pages.isNotEmpty() && !isLoadingPreview && !previewFailed

    val hasNormalizedPages get() = pages.any { it.normalize }

    val supportsOmr get() = musicXml == null && pages.isNotEmpty()

    val omrInput get() = OmrInput(key, pages.map { OmrPageInput(it.key, it.normalize, it.crop, it.erased) })

    val keepsStoredRecognition get() = omrEnabled && storedRecognition?.input == omrInput

    val isOmrRequested get() = omrEnabled && supportsOmr && !keepsStoredRecognition

    val recognized get() = (omr as? OmrState.Done)?.takeIf { isOmrRequested && it.input == omrInput }

    val isRecognizing get() = isRecognizing(isOmrRequested, omr, omrInput)

    val isPlayable get() = keepsStoredRecognition || recognized != null

    val omrDisplay: OmrDisplay
        get() {
            val stored = storedRecognition
            if (keepsStoredRecognition && stored != null) return stored.preview?.let(OmrDisplay::Recognized) ?: OmrDisplay.Recognizing(null)
            return omrDisplay(isOmrRequested, isPreviewReady, omr, omrInput)
        }

    val isComplete get() = if (musicXml != null) pages.isEmpty() else isPreviewReady && !isRecognizing

    val pageCount get() = if (musicXml != null) null else pages.size

    val recognitionEdit: RecognitionEdit
        get() {
            val music = recognized?.music
            return when {
                music != null -> RecognitionEdit.Replace(music)
                keepsStoredRecognition || storedRecognition == null -> RecognitionEdit.Unchanged
                else -> RecognitionEdit.Remove
            }
        }

    fun withPage(key: String, transform: (DraftPage) -> DraftPage) = copy(pages = pages.map { if (it.key == key) transform(it) else it })
}

fun Strings.contentSummary(musicXml: Boolean, pages: Int?, playable: Boolean): String {
    val summary = if (musicXml) musicXmlLabel else pageCount(pages ?: 0)
    return if (playable) "$summary · $omrPartSummary" else summary
}

fun PartDraft.summary(strings: Strings) = strings.contentSummary(musicXml != null, pageCount, isPlayable)

fun partNameTaken(name: String, otherNames: List<String>): Boolean {
    val trimmed = name.trim()
    return trimmed.isNotEmpty() && otherNames.any { it.trim().equals(trimmed, ignoreCase = true) }
}
