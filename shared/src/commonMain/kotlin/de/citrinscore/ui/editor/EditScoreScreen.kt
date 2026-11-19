package de.citrinscore.ui.editor

import androidx.compose.foundation.layout.Box
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
import de.citrinscore.ui.components.WizardScaffold
import de.citrinscore.ui.components.isLastStep
import de.citrinscore.ui.importer.ImportFilesStep
import de.citrinscore.ui.importer.partNameTaken
import de.citrinscore.ui.pages.ScoreDetails
import de.citrinscore.ui.pages.ScoreDetailsStep
import de.citrinscore.ui.parts.PartRowModel
import de.citrinscore.ui.parts.PartsStep
import de.citrinscore.ui.parts.partName
import de.citrinscore.ui.importer.contentSummary
import de.citrinscore.ui.importer.summary
import de.citrinscore.ui.theme.ContentWidth
import io.github.vinceglb.filekit.PlatformFile
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
        contentMaxWidth = { step -> if (step == EditStep.Files) ContentWidth.Wide else ContentWidth.Regular },
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
                EditStep.Files -> ImportFilesStep(draft, partNameTaken(draft.name, state.otherPartNames()), viewModel.editor)
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
        is EditablePart.Existing -> draft?.summary(strings) ?: strings.contentSummary(musicXml, pageCount, playable)
        is EditablePart.New -> draft.summary(strings)
    }
    return PartRowModel(strings.partName(name.trim(), index), summary)
}
