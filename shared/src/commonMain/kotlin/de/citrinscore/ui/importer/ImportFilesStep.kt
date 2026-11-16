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
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.pages.AddPagesButton
import de.citrinscore.ui.pages.PagePreview
import de.citrinscore.ui.pages.PageRow
import de.citrinscore.ui.pages.PagesLayout
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path

@Composable
fun ImportFilesStep(
    state: ImportState,
    onAdd: (List<PlatformFile>) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onScanThresholdChange: (Float) -> Unit,
) {
    val strings = LocalStrings.current
    val (title, subtitle) = when (state.type) {
        ImportType.Pdf -> strings.importPdfTitle to strings.importPdfSubtitle
        ImportType.Scan -> strings.importScanTitle to strings.importScanSubtitle
        ImportType.Images, null -> strings.importFilesTitle to strings.importFilesSubtitle
    }
    WizardStep(title, subtitle) {
        PagesLayout(
            list = { modifier -> FileList(state, onAdd, onMove, onRemove, modifier) },
            preview = { modifier ->
                if (state.files.isNotEmpty()) {
                    PagePreview(state.pages, state.isLoadingPreview, state.previewFailed, modifier) {
                        if (state.type == ImportType.Scan) ScanThresholdSlider(state.scanThreshold, onScanThresholdChange)
                    }
                }
            },
        )
    }
}

@Composable
private fun FileList(
    state: ImportState,
    onAdd: (List<PlatformFile>) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    val isPdf = state.type == ImportType.Pdf
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(state.type?.extensions.orEmpty()),
        mode = FileKitMode.Multiple(maxItems = if (isPdf) 1 else null),
    ) { picked ->
        picked?.let(onAdd)
    }
    val scanLauncher = rememberScanLauncher(onAdd)
    val (addLabel, onAddClick) = when (state.type) {
        ImportType.Pdf -> strings.choosePdf to picker::launch
        ImportType.Scan -> (if (scanUsesCamera) strings.takePhoto else strings.addScans) to scanLauncher
        ImportType.Images, null -> strings.addImages to picker::launch
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        state.files.forEachIndexed { index, file ->
            PageRow(
                position = index + 1,
                name = file.name,
                failed = file.path in state.failedPreviews,
                reorderable = !isPdf,
                onMoveUp = { onMove(index, index - 1) }.takeIf { index > 0 },
                onMoveDown = { onMove(index, index + 1) }.takeIf { index < state.files.lastIndex },
                onRemove = { onRemove(index) },
            )
        }
        AddPagesButton(addLabel, onAddClick)
    }
}

@Composable
private fun ScanThresholdSlider(threshold: Float, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        Text(LocalStrings.current.scanThreshold, style = MaterialTheme.typography.bodyLarge)
        Slider(value = threshold, onValueChange = onChange, valueRange = ScanThresholdRange, modifier = Modifier.weight(1f))
    }
}
