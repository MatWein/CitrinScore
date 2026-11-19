package de.citrinscore.ui.editor

import de.citrinscore.library.validateName
import de.citrinscore.ui.importer.PartDraft
import de.citrinscore.ui.importer.partNameTaken

enum class EditStep { Details, Files, Parts }

sealed interface EditablePart {
    val name: String

    data class Existing(
        val directory: String,
        override val name: String,
        val musicXml: Boolean,
        val pageCount: Int?,
        val playable: Boolean,
        val draft: PartDraft? = null,
    ) : EditablePart

    data class New(val draft: PartDraft) : EditablePart {
        override val name get() = draft.name
    }
}

data class EditScoreState(
    val step: EditStep = EditStep.Details,
    val isLoaded: Boolean = false,
    val loadFailed: Boolean = false,
    val parts: List<EditablePart> = emptyList(),
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
        EditStep.Details -> isLoaded && titleError == null
        EditStep.Files -> draft.isComplete && !partNameTaken(draft.name, otherPartNames())
        EditStep.Parts -> !isSaving && parts.isNotEmpty()
    }
}
