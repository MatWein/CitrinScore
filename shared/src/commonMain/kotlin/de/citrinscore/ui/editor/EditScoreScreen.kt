package de.citrinscore.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.library.LibraryRepository
import de.citrinscore.ui.components.AddButton
import de.citrinscore.ui.components.NumberedRow
import de.citrinscore.ui.components.WizardScaffold
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.components.isLastStep
import de.citrinscore.ui.importer.ImportFilesStep
import de.citrinscore.ui.importer.ImportType
import de.citrinscore.ui.importer.ImportTypeStep
import de.citrinscore.ui.importer.partNameTaken
import de.citrinscore.ui.importer.title
import de.citrinscore.ui.pages.PagePreview
import de.citrinscore.ui.pages.PagesLayout
import de.citrinscore.ui.pages.ScoreDetails
import de.citrinscore.ui.pages.ScoreDetailsStep
import de.citrinscore.ui.parts.PartNameField
import de.citrinscore.ui.parts.PartRowModel
import de.citrinscore.ui.parts.PartsStep
import de.citrinscore.ui.parts.partName
import de.citrinscore.ui.importer.OmrSection
import de.citrinscore.ui.importer.summary
import de.citrinscore.ui.parts.partSummary
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path

private const val INDICATOR_STEPS = 2

@Composable
fun EditScoreScreen(
    folder: PlatformFile,
    score: PlatformFile,
    repository: LibraryRepository,
    onClose: () -> Unit,
    onSaved: (PlatformFile) -> Unit,
) {
    val viewModel = viewModel(key = "${score.path}/edit") { EditScoreViewModel(folder, score, repository) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val draft by viewModel.editor.draft.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    val isFirstStep = state.step == EditStep.entries.first()

    WizardScaffold(
        step = state.step,
        stepCount = INDICATOR_STEPS,
        stepIndex = if (isFirstStep) 0 else 1,
        backLabel = if (isFirstStep) strings.cancel else strings.back,
        onBack = if (isFirstStep) onClose else viewModel::back,
        nextLabel = if (state.step.isLastStep()) strings.save else strings.next,
        nextEnabled = state.canAdvance(draft),
        onNext = { viewModel.next(onSaved) },
        contentMaxWidth = { step -> if (step == EditStep.Pages || step == EditStep.Files) ContentWidth.Wide else ContentWidth.Regular },
    ) { step ->
        when {
            state.loadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.editLoadFailed, color = MaterialTheme.colorScheme.error)
            }
            else -> when (step) {
                EditStep.Details -> ScoreDetailsStep(
                    stepTitle = strings.editDetailsTitle,
                    stepSubtitle = strings.editDetailsSubtitle,
                    details = ScoreDetails(state.title, state.composer, state.description, state.titleError),
                    failure = null,
                    onTitleChange = viewModel::updateTitle,
                    onComposerChange = viewModel::updateComposer,
                    onDescriptionChange = viewModel::updateDescription,
                )
                EditStep.Type -> ImportTypeStep(draft.type, viewModel.editor::selectType)
                EditStep.Files -> ImportFilesStep(draft, partNameTaken(draft.name, state.otherPartNames()), viewModel.editor)
                EditStep.Pages -> EditPagesStep(state, viewModel)
                EditStep.Parts -> PartsStep(
                    subtitle = strings.editPartsSubtitle,
                    parts = state.parts.mapIndexed { index, part -> part.row(strings, index) },
                    failure = strings.editSaveFailed.takeIf { state.saveFailed },
                    onAdd = viewModel::addPart,
                    onEdit = viewModel::editPart,
                    onMove = viewModel::movePart,
                    onRemove = viewModel::removePart,
                )
            }
        }
    }
}

private fun EditablePart.row(strings: Strings, index: Int): PartRowModel {
    val summary = when (this) {
        is EditablePart.Existing -> strings.partSummary(type.title(strings), pages?.size ?: pageCount).let { if (isPlayable) "$it · ${strings.omrPartSummary}" else it }
        is EditablePart.New -> draft.summary(strings)
    }
    return PartRowModel(strings.partName(name.trim(), index), summary)
}

@Composable
private fun EditPagesStep(state: EditScoreState, viewModel: EditScoreViewModel) {
    val strings = LocalStrings.current
    val draft = state.pagesDraft ?: return
    val picker = rememberFilePickerLauncher(FileKitType.File(ImportType.Images.extensions), FileKitMode.Multiple()) { files ->
        files?.let(viewModel::addImages)
    }
    val (title, subtitle) = if (draft.canEditPages) strings.editPagesTitle to strings.editPagesSubtitle else strings.editPartTitle to strings.editPartSubtitle
    WizardStep(title, subtitle) {
        PartNameField(draft.name, partNameTaken(draft.name, state.otherPartNames()), viewModel::updatePartName)
        if (!draft.canEditPages) return@WizardStep
        PagesLayout(
            list = { modifier ->
                Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    draft.pages.forEachIndexed { index, page ->
                        NumberedRow(
                            position = index + 1,
                            name = when (page) {
                                is EditablePage.Existing -> strings.pageNumber(page.index + 1)
                                is EditablePage.Added -> page.file.name
                            },
                            reorderable = true,
                            onMoveUp = { viewModel.movePage(index, index - 1) }.takeIf { index > 0 },
                            onMoveDown = { viewModel.movePage(index, index + 1) }.takeIf { index < draft.pages.lastIndex },
                            onRemove = { viewModel.removePage(index) },
                            failed = previewKey(draft.directory, page) in state.failedPreviews,
                        )
                    }
                    if (draft.canAddPages) AddButton(strings.addImages, picker::launch)
                }
            },
            preview = { modifier ->
                Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.Section)) {
                    PagePreview(state.previewPages, state.isLoadingPreview, state.previewFailed, Modifier.fillMaxWidth())
                    if (draft.supportsOmr) OmrSection(draft.omrEnabled, state.omrDisplay, viewModel::enableOmr, Modifier.fillMaxWidth())
                }
            },
        )
    }
}
