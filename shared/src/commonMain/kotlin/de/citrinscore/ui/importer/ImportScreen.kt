package de.citrinscore.ui.importer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.library.LibraryRepository
import de.citrinscore.ui.components.WizardScaffold
import de.citrinscore.ui.components.isLastStep
import de.citrinscore.ui.pages.ScoreDetails
import de.citrinscore.ui.pages.ScoreDetailsStep
import de.citrinscore.ui.parts.PartRowModel
import de.citrinscore.ui.parts.PartsStep
import de.citrinscore.ui.parts.partName
import de.citrinscore.ui.parts.partSummary
import de.citrinscore.ui.theme.ContentWidth
import io.github.vinceglb.filekit.PlatformFile

@Composable
fun ImportScreen(
    folder: PlatformFile,
    repository: LibraryRepository,
    onClose: () -> Unit,
    onImported: () -> Unit,
) {
    val viewModel = viewModel { ImportViewModel(folder, repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.editor.draft.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val isFirstStep = state.step == ImportStep.entries.first()

    WizardScaffold(
        step = state.step,
        stepCount = ImportStep.entries.size,
        backLabel = if (isFirstStep) strings.cancel else strings.back,
        onBack = if (isFirstStep) onClose else viewModel::back,
        nextLabel = if (state.step.isLastStep()) strings.importAction else strings.next,
        nextEnabled = state.canAdvance(draft),
        onNext = { viewModel.next(onImported) },
        contentMaxWidth = { step -> if (step == ImportStep.Files) ContentWidth.Wide else ContentWidth.Regular },
    ) { step ->
        when (step) {
            ImportStep.Details -> ScoreDetailsStep(
                stepTitle = strings.importMetadataTitle,
                stepSubtitle = strings.importMetadataSubtitle,
                details = ScoreDetails(state.title, state.composer, state.description, state.titleError),
                failure = null,
                onTitleChange = viewModel::updateTitle,
                onComposerChange = viewModel::updateComposer,
                onDescriptionChange = viewModel::updateDescription,
            )
            ImportStep.Type -> ImportTypeStep(draft.type, viewModel.editor::selectType)
            ImportStep.Files -> ImportFilesStep(draft, partNameTaken(draft.name, state.otherPartNames()), viewModel.editor)
            ImportStep.Parts -> PartsStep(
                subtitle = strings.importPartsSubtitle,
                parts = state.parts.mapIndexed { index, part -> part.row(strings, index) },
                failure = strings.importFailed.takeIf { state.saveFailed },
                onAdd = viewModel::addPart,
                onEdit = viewModel::editPart,
                onMove = viewModel::movePart,
                onRemove = viewModel::removePart,
            )
        }
    }
}

private fun PartDraft.row(strings: Strings, index: Int) =
    PartRowModel(strings.partName(name.trim(), index), strings.partSummary(type?.title(strings).orEmpty(), pageCount))
