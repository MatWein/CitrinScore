package de.citrinscore.ui.importer

import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.library.validateName
import io.github.vinceglb.filekit.PlatformFile

enum class ImportStep { Type, Files, Preview, Metadata }

data class ImportState(
    val step: ImportStep = ImportStep.Type,
    val type: ImportType? = null,
    val files: List<PlatformFile> = emptyList(),
    val pages: List<ImageBitmap> = emptyList(),
    val isLoadingPreview: Boolean = false,
    val previewFailed: Boolean = false,
    val title: String = "",
    val composer: String = "",
    val description: String = "",
    val takenTitles: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val titleError get() = validateName(title, takenTitles)

    val canAdvance
        get() = when (step) {
            ImportStep.Type -> type?.isAvailable == true
            ImportStep.Files -> files.isNotEmpty()
            ImportStep.Preview -> !isLoadingPreview && !previewFailed && pages.isNotEmpty()
            ImportStep.Metadata -> !isSaving && titleError == null
        }
}
