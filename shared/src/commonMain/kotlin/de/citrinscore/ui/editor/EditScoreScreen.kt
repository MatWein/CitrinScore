package de.citrinscore.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import de.citrinscore.library.LibraryRepository
import de.citrinscore.ui.components.WizardScaffold
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.components.isLastStep
import de.citrinscore.ui.importer.ImportType
import de.citrinscore.ui.pages.AddPagesButton
import de.citrinscore.ui.pages.PagePreview
import de.citrinscore.ui.pages.PageRow
import de.citrinscore.ui.pages.PagesLayout
import de.citrinscore.ui.pages.ScoreDetails
import de.citrinscore.ui.pages.ScoreDetailsStep
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path

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
    val strings = LocalStrings.current
    val isFirstStep = state.step == EditStep.entries.first()

    WizardScaffold(
        step = state.step,
        stepCount = EditStep.entries.size,
        backLabel = if (isFirstStep) strings.cancel else strings.back,
        onBack = if (isFirstStep) onClose else viewModel::back,
        nextLabel = if (state.step.isLastStep()) strings.save else strings.next,
        nextEnabled = state.canAdvance,
        onNext = { viewModel.next(onSaved) },
        contentMaxWidth = { step -> if (step == EditStep.Pages) ContentWidth.Wide else ContentWidth.Regular },
    ) { step ->
        when {
            state.loadFailed -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(strings.editLoadFailed, color = MaterialTheme.colorScheme.error)
            }
            step == EditStep.Pages -> EditPagesStep(state, viewModel)
            else -> ScoreDetailsStep(
                stepTitle = strings.editDetailsTitle,
                stepSubtitle = strings.editDetailsSubtitle,
                details = ScoreDetails(state.title, state.composer, state.description, state.titleError),
                failure = strings.editSaveFailed.takeIf { state.saveFailed },
                onTitleChange = viewModel::updateTitle,
                onComposerChange = viewModel::updateComposer,
                onDescriptionChange = viewModel::updateDescription,
            )
        }
    }
}

@Composable
private fun EditPagesStep(state: EditScoreState, viewModel: EditScoreViewModel) {
    val strings = LocalStrings.current
    val picker = rememberFilePickerLauncher(FileKitType.File(ImportType.Images.extensions), FileKitMode.Multiple()) { files ->
        files?.let(viewModel::addImages)
    }
    WizardStep(strings.editPagesTitle, strings.editPagesSubtitle) {
        PagesLayout(
            list = { modifier ->
                Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                    state.pages.forEachIndexed { index, page ->
                        PageRow(
                            position = index + 1,
                            name = when (page) {
                                is EditablePage.Existing -> strings.pageNumber(page.index + 1)
                                is EditablePage.Added -> page.file.name
                            },
                            failed = page.key in state.failedPreviews,
                            reorderable = true,
                            onMoveUp = { viewModel.movePage(index, index - 1) }.takeIf { index > 0 },
                            onMoveDown = { viewModel.movePage(index, index + 1) }.takeIf { index < state.pages.lastIndex },
                            onRemove = { viewModel.removePage(index) },
                        )
                    }
                    if (state.canAddPages) AddPagesButton(strings.addImages, picker::launch)
                }
            },
            preview = { modifier -> PagePreview(state.previewPages, state.isLoadingPreview, state.previewFailed, modifier) },
        )
    }
}
