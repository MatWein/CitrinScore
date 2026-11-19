package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_crop
import de.citrinscore.resources.ic_ink_eraser
import de.citrinscore.resources.ic_undo
import de.citrinscore.score.scan.ScanThresholdRange
import de.citrinscore.ui.components.AddButton
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.NumberedRow
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.pages.PagesLayout
import de.citrinscore.ui.pages.PreviewFrame
import de.citrinscore.ui.parts.PartNameField
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import org.jetbrains.compose.resources.painterResource

private const val DEFAULT_ERASER_WIDTH = 0.02f
private const val LABEL_ALPHA = 0.7f
private val EraserWidthRange = 0.005f..0.08f

@Composable
fun ImportFilesStep(draft: PartDraft, nameTaken: Boolean, editor: PartDraftEditor) {
    val strings = LocalStrings.current
    WizardStep(strings.importFilesTitle, strings.importFilesSubtitle) {
        PartNameField(draft.name, nameTaken, editor::updateName)
        PagesLayout(
            list = { modifier -> SourceList(draft, editor, modifier) },
            preview = { modifier ->
                if (draft.pages.isNotEmpty()) {
                    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.Section)) {
                        PagesPanel(draft, editor, Modifier.fillMaxWidth())
                        OmrSection(draft.omrEnabled, draft.omrDisplay, editor::enableOmr, Modifier.fillMaxWidth())
                    }
                }
            },
        )
    }
}

@Composable
private fun SourceList(draft: PartDraft, editor: PartDraftEditor, modifier: Modifier) {
    val strings = LocalStrings.current
    val picker = rememberFilePickerLauncher(
        type = importPickerExtensions?.let { FileKitType.File(it) } ?: FileKitType.File(),
        mode = FileKitMode.Multiple(),
    ) { picked -> picked?.let { editor.addFiles(it) } }
    val camera = rememberCameraLauncher { photo -> editor.addFiles(listOf(photo), fromCamera = true) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        draft.musicXml?.let { musicXml ->
            NumberedRow(position = 1, name = musicXml.file.name, reorderable = false, onMoveUp = null, onMoveDown = null, onRemove = editor::removeMusicXml)
        }
        draft.pages.forEachIndexed { index, page ->
            NumberedRow(
                position = index + 1,
                name = page.label,
                subtitle = strings.pageNumber(page.page + 1).takeIf { page.pageCount > 1 },
                reorderable = true,
                onMoveUp = { editor.movePage(index, index - 1) }.takeIf { index > 0 },
                onMoveDown = { editor.movePage(index, index + 1) }.takeIf { index < draft.pages.lastIndex },
                onRemove = { editor.removePage(index) },
                failed = page.failed,
            )
        }
        AddButton(strings.addFiles, picker::launch)
        camera?.let { AddButton(strings.takePhoto, it) }
    }
}

@Composable
private fun PagesPanel(draft: PartDraft, editor: PartDraftEditor, modifier: Modifier) {
    val strings = LocalStrings.current
    var tool by rememberSaveable { mutableStateOf(PageTool.Crop) }
    var eraserWidth by rememberSaveable { mutableFloatStateOf(DEFAULT_ERASER_WIDTH) }
    PreviewFrame(
        isLoading = draft.isLoadingPreview,
        failed = draft.previewFailed,
        modifier = modifier,
        controls = {
            if (draft.hasNormalizedPages) {
                LabeledSlider(strings.scanThreshold, draft.threshold, ScanThresholdRange, editor::updateThreshold)
                ToolSelector(tool) { tool = it }
                if (tool == PageTool.Eraser) {
                    LabeledSlider(strings.eraserSize, eraserWidth, EraserWidthRange, { eraserWidth = it }) {
                        IconAction(Res.drawable.ic_undo, strings.undo, onClick = editor::undoErase, enabled = draft.erasedPages.isNotEmpty())
                    }
                }
            }
        },
    ) {
        draft.pages.forEach { page ->
            key(page.key) {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    NormalizeSwitch(page.normalize) { editor.setNormalize(page.key, it) }
                    page.view?.let { view ->
                        PageEditor(
                            page = view,
                            tool = if (page.normalize) tool else PageTool.Crop,
                            eraserWidth = eraserWidth,
                            onCrop = { editor.cropPage(page.key, it) },
                            onErase = { editor.erasePage(page.key, it) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NormalizeSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(LocalStrings.current.normalizePage, style = MaterialTheme.typography.bodyMedium, color = Color.Black.copy(alpha = LABEL_ALPHA))
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun ToolSelector(tool: PageTool, onSelect: (PageTool) -> Unit) {
    val strings = LocalStrings.current
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        PageTool.entries.forEachIndexed { index, entry ->
            SegmentedButton(
                selected = tool == entry,
                onClick = { onSelect(entry) },
                shape = SegmentedButtonDefaults.itemShape(index, PageTool.entries.size),
                icon = {
                    val icon = when (entry) {
                        PageTool.Crop -> Res.drawable.ic_crop
                        PageTool.Eraser -> Res.drawable.ic_ink_eraser
                    }
                    Icon(painterResource(icon), contentDescription = null)
                },
            ) {
                Text(
                    when (entry) {
                        PageTool.Crop -> strings.scanCrop
                        PageTool.Eraser -> strings.scanEraser
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
