package de.citrinscore.ui.importer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.library.LibraryRepository
import de.citrinscore.ui.components.ContentMaxWidth
import de.citrinscore.ui.components.WideContentMaxWidth
import de.citrinscore.ui.components.WizardScaffold
import de.citrinscore.ui.components.isLastStep
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
    val strings = LocalStrings.current
    val isFirstStep = state.step == ImportStep.entries.first()

    WizardScaffold(
        step = state.step,
        stepCount = ImportStep.entries.size,
        backLabel = if (isFirstStep) strings.cancel else strings.back,
        onBack = if (isFirstStep) onClose else viewModel::back,
        nextLabel = if (state.step.isLastStep()) strings.importAction else strings.next,
        nextEnabled = state.canAdvance,
        onNext = { viewModel.next(onImported) },
        contentMaxWidth = { step -> if (step == ImportStep.Files) WideContentMaxWidth else ContentMaxWidth },
    ) { step ->
        when (step) {
            ImportStep.Type -> ImportTypeStep(state.type, viewModel::selectType)
            ImportStep.Files -> ImportFilesStep(
                state = state,
                onAdd = viewModel::addFiles,
                onMove = viewModel::moveFile,
                onRemove = viewModel::removeFile,
            )
            ImportStep.Metadata -> ImportMetadataStep(
                state = state,
                onTitleChange = viewModel::updateTitle,
                onComposerChange = viewModel::updateComposer,
                onDescriptionChange = viewModel::updateDescription,
            )
        }
    }
}
