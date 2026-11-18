package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_crop
import de.citrinscore.resources.ic_ink_eraser
import de.citrinscore.resources.ic_undo
import de.citrinscore.score.scan.ScanThresholdRange
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.pages.PreviewFrame
import de.citrinscore.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource

private const val DEFAULT_ERASER_WIDTH = 0.02f
private val EraserWidthRange = 0.005f..0.08f

@Composable
fun ScanPreviewPanel(draft: PartDraft, editor: PartDraftEditor, modifier: Modifier) {
    val strings = LocalStrings.current
    var tool by rememberSaveable { mutableStateOf(ScanTool.Crop) }
    var eraserWidth by rememberSaveable { mutableFloatStateOf(DEFAULT_ERASER_WIDTH) }
    PreviewFrame(
        isLoading = draft.isLoadingPreview,
        failed = draft.previewFailed,
        modifier = modifier,
        controls = {
            LabeledSlider(strings.scanThreshold, draft.scanThreshold, ScanThresholdRange, editor::updateScanThreshold)
            ScanToolSelector(tool) { tool = it }
            if (tool == ScanTool.Eraser) {
                LabeledSlider(strings.eraserSize, eraserWidth, EraserWidthRange, { eraserWidth = it }) {
                    IconAction(Res.drawable.ic_undo, strings.undo, onClick = editor::undoErase, enabled = draft.erasedPages.isNotEmpty())
                }
            }
        },
    ) {
        draft.scanPages.forEach { (page, scan) ->
            key(page) {
                ScanPageEditor(
                    scan = scan,
                    tool = tool,
                    eraserWidth = eraserWidth,
                    onCrop = { editor.cropScan(page, it) },
                    onErase = { editor.eraseScan(page, it) },
                )
            }
        }
    }
}

@Composable
private fun ScanToolSelector(tool: ScanTool, onSelect: (ScanTool) -> Unit) {
    val strings = LocalStrings.current
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ScanTool.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = tool == entry,
                onClick = { onSelect(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, ScanTool.entries.size),
                icon = {
                    val icon = when (entry) {
                        ScanTool.Crop -> Res.drawable.ic_crop
                        ScanTool.Eraser -> Res.drawable.ic_ink_eraser
                    }
                    Icon(painterResource(icon), contentDescription = null)
                },
            ) {
                Text(
                    when (entry) {
                        ScanTool.Crop -> strings.scanCrop
                        ScanTool.Eraser -> strings.scanEraser
                    },
                )
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    trailing: @Composable () -> Unit = {},
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Slider(value = value, onValueChange = onChange, valueRange = range, modifier = Modifier.weight(1f))
        trailing()
    }
}
