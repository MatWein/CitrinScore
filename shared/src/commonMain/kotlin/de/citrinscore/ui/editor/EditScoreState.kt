package de.citrinscore.ui.editor

import de.citrinscore.library.validateName
import de.citrinscore.score.PageCrop
import de.citrinscore.ui.importer.ImportType
import de.citrinscore.ui.importer.OmrDisplay
import de.citrinscore.ui.importer.OmrState
import de.citrinscore.ui.importer.isRecognizing
import de.citrinscore.ui.importer.omrDisplay
import de.citrinscore.ui.importer.PartDraft
import de.citrinscore.ui.importer.partNameTaken
import de.citrinscore.ui.pages.PreviewPage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

enum class EditStep { Details, Type, Files, Pages, Parts }

sealed interface EditablePage {
    val key: String

    data class Existing(val index: Int) : EditablePage {
        override val key get() = "existing/$index"
    }

    data class Added(val file: PlatformFile) : EditablePage {
        override val key get() = "added/${file.path}"
    }
}

sealed interface EditablePart {
    val name: String

    data class Existing(
        val directory: String,
        override val name: String,
        val type: ImportType,
        val pageCount: Int?,
        val pages: List<EditablePage>? = null,
        val storedRecognition: Boolean = false,
        val omrEnabled: Boolean = storedRecognition,
        val omr: OmrState.Done? = null,
    ) : EditablePart {
        val isPlayable get() = omrEnabled && (omr != null || storedRecognition)
    }

    data class New(val draft: PartDraft) : EditablePart {
        override val name get() = draft.name
    }
}

data class PagesDraft(
    val directory: String,
    val name: String,
    val type: ImportType,
    val pages: List<EditablePage>,
    val originalPages: List<EditablePage> = pages,
    val storedRecognition: Boolean = false,
    val storedPreview: List<PreviewPage>? = null,
    val omrEnabled: Boolean = false,
    val omr: OmrState? = null,
) {
    val canEditPages get() = type != ImportType.MusicXml

    val supportsOmr get() = type.supportsOmr

    val keepsStoredRecognition get() = omrEnabled && storedRecognition && pages == originalPages

    val needsRecognition get() = omrEnabled && supportsOmr && !keepsStoredRecognition

    val canAddPages get() = type == ImportType.Images
}

data class EditScoreState(
    val step: EditStep = EditStep.Details,
    val isLoaded: Boolean = false,
    val loadFailed: Boolean = false,
    val parts: List<EditablePart> = emptyList(),
    val editingPart: Int? = null,
    val pagesDraft: PagesDraft? = null,
    val previews: Map<String, PreviewPage> = emptyMap(),
    val failedPreviews: Set<String> = emptySet(),
    val title: String = "",
    val composer: String = "",
    val description: String = "",
    val takenTitles: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val titleError get() = validateName(title, takenTitles)

    fun otherPartNames() = parts.filterIndexed { index, _ -> index != editingPart }.map { it.name }

    private val draftPreviewKeys get() = pagesDraft?.let { draft -> draft.pages.map { previewKey(draft.directory, it) } }.orEmpty()

    val previewPages get() = draftPreviewKeys.mapNotNull { previews[it] }

    val isLoadingPreview get() = draftPreviewKeys.any { it !in previews && it !in failedPreviews }

    val previewFailed get() = draftPreviewKeys.any { it in failedPreviews }

    val omrInput get() = pagesDraft?.let { draft -> EditOmrInput(draft.directory, draft.pages, previewPages.map { it.crop }) }

    private val isPreviewReady get() = pagesDraft?.pages?.isNotEmpty() == true && !isLoadingPreview && !previewFailed

    val omrRequest get() = omrInput?.takeIf { pagesDraft?.needsRecognition == true && isPreviewReady }

    val omrDisplay: OmrDisplay
        get() {
            val draft = pagesDraft ?: return OmrDisplay.Idle
            if (draft.keepsStoredRecognition) return draft.storedPreview?.let(OmrDisplay::Recognized) ?: OmrDisplay.Recognizing(null)
            return omrDisplay(draft.needsRecognition, isPreviewReady, draft.omr, omrInput)
        }

    val recognizedPages get() = (pagesDraft?.omr as? OmrState.Done)?.takeIf { it.input == omrInput }

    fun canAdvance(draft: PartDraft) = when (step) {
        EditStep.Details -> isLoaded && titleError == null
        EditStep.Type -> draft.type != null
        EditStep.Files -> draft.isComplete && !partNameTaken(draft.name, otherPartNames())
        EditStep.Pages -> pagesDraft != null && !partNameTaken(pagesDraft.name, otherPartNames()) &&
            (!pagesDraft.canEditPages || pagesDraft.pages.isNotEmpty() && !isLoadingPreview && !previewFailed) &&
            !isRecognizing(pagesDraft.needsRecognition, pagesDraft.omr, omrInput)
        EditStep.Parts -> !isSaving && parts.isNotEmpty()
    }
}

fun previewKey(directory: String, page: EditablePage) = "$directory/${page.key}"

data class EditOmrInput(val directory: String, val pages: List<EditablePage>, val crops: List<PageCrop>)
