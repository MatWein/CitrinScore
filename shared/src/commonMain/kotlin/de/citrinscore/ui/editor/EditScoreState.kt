package de.citrinscore.ui.editor

import de.citrinscore.library.validateName
import de.citrinscore.ui.pages.PreviewPage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

enum class EditStep { Pages, Details }

sealed interface EditablePage {
    val key: String

    data class Existing(val index: Int) : EditablePage {
        override val key get() = "existing/$index"
    }

    data class Added(val file: PlatformFile) : EditablePage {
        override val key get() = "added/${file.path}"
    }
}

data class EditScoreState(
    val step: EditStep = EditStep.Pages,
    val isLoaded: Boolean = false,
    val loadFailed: Boolean = false,
    val canAddPages: Boolean = false,
    val pages: List<EditablePage> = emptyList(),
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

    val previewPages get() = pages.mapNotNull { previews[it.key] }

    val isLoadingPreview get() = pages.any { it.key !in previews && it.key !in failedPreviews }

    val previewFailed get() = pages.any { it.key in failedPreviews }

    val canAdvance
        get() = when (step) {
            EditStep.Pages -> isLoaded && pages.isNotEmpty() && !isLoadingPreview && !previewFailed
            EditStep.Details -> !isSaving && titleError == null
        }
}
