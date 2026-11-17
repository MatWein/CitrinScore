package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.score.scan.ScanThresholdRange
import de.citrinscore.ui.components.AddButton
import de.citrinscore.ui.components.NumberedRow
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.pages.PagePreview
import de.citrinscore.ui.pages.PagesLayout
import de.citrinscore.ui.parts.PartNameField
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path

@Composable
fun ImportFilesStep(draft: PartDraft, nameTaken: Boolean, editor: PartDraftEditor) {
    val strings = LocalStrings.current
    val (title, subtitle) = when (draft.type) {
        ImportType.Pdf -> strings.importPdfTitle to strings.importPdfSubtitle
        ImportType.Scan -> strings.importScanTitle to strings.importScanSubtitle
        ImportType.Images, null -> strings.importFilesTitle to strings.importFilesSubtitle
    }
    WizardStep(title, subtitle) {
        PartNameField(draft.name, nameTaken, editor::updateName)
        PagesLayout(
            list = { modifier -> FileList(draft, editor, modifier) },
            preview = { modifier ->
                if (draft.files.isNotEmpty()) {
                    PagePreview(draft.pages, draft.isLoadingPreview, draft.previewFailed, modifier) {
                        if (draft.type == ImportType.Scan) ScanThresholdSlider(draft.scanThreshold, editor::updateScanThreshold)
                    }
                }
            },
        )
    }
}

@Composable
private fun FileList(draft: PartDraft, editor: PartDraftEditor, modifier: Modifier) {
    val strings = LocalStrings.current
    val isPdf = draft.type == ImportType.Pdf
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(draft.type?.extensions.orEmpty()),
        mode = FileKitMode.Multiple(maxItems = if (isPdf) 1 else null),
    ) { picked ->
        picked?.let(editor::addFiles)
    }
    val scanLauncher = rememberScanLauncher(editor::addFiles)
    val (addLabel, onAddClick) = when (draft.type) {
        ImportType.Pdf -> strings.choosePdf to picker::launch
        ImportType.Scan -> (if (scanUsesCamera) strings.takePhoto else strings.addScans) to scanLauncher
        ImportType.Images, null -> strings.addImages to picker::launch
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        draft.files.forEachIndexed { index, file ->
            NumberedRow(
                position = index + 1,
                name = file.name,
                reorderable = !isPdf,
                onMoveUp = { editor.moveFile(index, index - 1) }.takeIf { index > 0 },
                onMoveDown = { editor.moveFile(index, index + 1) }.takeIf { index < draft.files.lastIndex },
                onRemove = { editor.removeFile(index) },
                failed = file.path in draft.failedPreviews,
            )
        }
        AddButton(addLabel, onAddClick)
    }
}

@Composable
private fun ScanThresholdSlider(threshold: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        Text(LocalStrings.current.scanThreshold, style = MaterialTheme.typography.bodyLarge)
        Slider(value = threshold, onValueChange = onChange, valueRange = ScanThresholdRange, modifier = Modifier.weight(1f))
    }
}
