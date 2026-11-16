package de.citrinscore.ui.importer

import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.library.validateName
import de.citrinscore.score.PageCrop
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

enum class ImportStep { Type, Files, Metadata }

data class PreviewPage(val image: ImageBitmap, val crop: PageCrop)

data class ImportState(
    val step: ImportStep = ImportStep.Type,
    val type: ImportType? = null,
    val files: List<PlatformFile> = emptyList(),
    val previews: Map<String, List<PreviewPage>> = emptyMap(),
    val failedPreviews: Set<String> = emptySet(),
    val title: String = "",
    val composer: String = "",
    val description: String = "",
    val takenTitles: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val titleError get() = validateName(title, takenTitles)

    val pages get() = files.flatMap { previews[it.path].orEmpty() }

    val isLoadingPreview get() = files.any { it.path !in previews && it.path !in failedPreviews }

    val previewFailed get() = files.any { it.path in failedPreviews }

    val canAdvance
        get() = when (step) {
            ImportStep.Type -> type?.isAvailable == true
            ImportStep.Files -> files.isNotEmpty() && !isLoadingPreview && !previewFailed
            ImportStep.Metadata -> !isSaving && titleError == null
        }
}
