package de.citrinscore.ui.importer

import de.citrinscore.library.validateName

enum class ImportStep { Details, Files, Parts }

data class ImportState(
    val step: ImportStep = ImportStep.Details,
    val parts: List<PartDraft> = emptyList(),
    val editingPart: Int? = null,
    val title: String = "",
    val composer: String = "",
    val description: String = "",
    val takenTitles: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val saveFailed: Boolean = false,
) {
    val titleError get() = validateName(title, takenTitles)

    fun otherPartNames() = parts.filterIndexed { index, _ -> index != editingPart }.map { it.name }

    fun canAdvance(draft: PartDraft) = when (step) {
        ImportStep.Details -> titleError == null
        ImportStep.Files -> draft.isComplete && !partNameTaken(draft.name, otherPartNames())
        ImportStep.Parts -> !isSaving && parts.isNotEmpty()
    }
}
