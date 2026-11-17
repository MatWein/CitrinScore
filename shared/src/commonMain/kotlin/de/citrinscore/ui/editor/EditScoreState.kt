package de.citrinscore.ui.editor

import de.citrinscore.library.validateName
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
        val isPdf: Boolean,
        val pageCount: Int,
        val pages: List<EditablePage>? = null,
    ) : EditablePart

    data class New(val draft: PartDraft) : EditablePart {
        override val name get() = draft.name
    }
}

data class PagesDraft(
    val directory: String,
    val name: String,
    val canAddPages: Boolean,
    val pages: List<EditablePage>,
)

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

    fun canAdvance(draft: PartDraft) = when (step) {
        EditStep.Details -> isLoaded && titleError == null
        EditStep.Type -> draft.type != null
        EditStep.Files -> draft.isComplete && !partNameTaken(draft.name, otherPartNames())
        EditStep.Pages -> pagesDraft != null && pagesDraft.pages.isNotEmpty() && !isLoadingPreview && !previewFailed &&
            !partNameTaken(pagesDraft.name, otherPartNames())
        EditStep.Parts -> !isSaving && parts.isNotEmpty()
    }
}

fun previewKey(directory: String, page: EditablePage) = "$directory/${page.key}"
