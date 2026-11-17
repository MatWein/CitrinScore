package de.citrinscore.ui.viewer.annotations

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledTonalIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_check
import de.citrinscore.resources.ic_draw
import de.citrinscore.resources.ic_ink_eraser
import de.citrinscore.resources.ic_text_fields
import de.citrinscore.resources.ic_undo
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.LabelTooltip
import de.citrinscore.ui.components.MessageDialog
import de.citrinscore.ui.components.TextInputDialog
import de.citrinscore.ui.theme.Spacing
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val ToolbarElevation = 6.dp

@Composable
fun AnnotationToolbar(state: AnnotationState, viewModel: AnnotationViewModel, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = ToolbarElevation,
    ) {
        Row(Modifier.padding(horizontal = Spacing.Small), verticalAlignment = Alignment.CenterVertically) {
            ToolButton(AnnotationTool.Pen, Res.drawable.ic_draw, strings.annotationPen, state.tool, viewModel::selectTool)
            ToolButton(AnnotationTool.Text, Res.drawable.ic_text_fields, strings.annotationText, state.tool, viewModel::selectTool)
            ToolButton(AnnotationTool.Eraser, Res.drawable.ic_ink_eraser, strings.annotationEraser, state.tool, viewModel::selectTool)
            IconAction(Res.drawable.ic_undo, strings.undo, onClick = viewModel::undo, enabled = state.canUndo)
            IconAction(Res.drawable.ic_check, strings.done, onClick = viewModel::toggleEditing)
        }
    }
}

@Composable
fun AnnotationDialogs(state: AnnotationState, viewModel: AnnotationViewModel) {
    val strings = LocalStrings.current
    state.draft?.let { draft ->
        TextInputDialog(
            title = strings.textNoteTitle,
            label = strings.textNoteLabel,
            initialText = draft.original?.text.orEmpty(),
            confirmLabel = strings.ok,
            allowBlank = draft.original != null,
            onConfirm = viewModel::commitText,
            onDismiss = viewModel::cancelText,
            singleLine = false,
        )
    }
    if (state.saveFailed) {
        MessageDialog(strings.annotationsFailedTitle, strings.annotationsSaveFailed, viewModel::dismissSaveFailure)
    }
}

@Composable
private fun ToolButton(
    tool: AnnotationTool,
    icon: DrawableResource,
    label: String,
    selected: AnnotationTool,
    onSelect: (AnnotationTool) -> Unit,
) {
    LabelTooltip(label) {
        FilledTonalIconToggleButton(checked = tool == selected, onCheckedChange = { onSelect(tool) }) {
            Icon(painterResource(icon), contentDescription = label)
        }
    }
}
