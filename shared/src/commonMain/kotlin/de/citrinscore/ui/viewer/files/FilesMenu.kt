package de.citrinscore.ui.viewer.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_add
import de.citrinscore.resources.ic_attach_file
import de.citrinscore.resources.ic_delete
import de.citrinscore.resources.ic_description
import de.citrinscore.resources.ic_download
import de.citrinscore.score.Attachment
import de.citrinscore.ui.components.ConfirmDeleteDialog
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.MessageDialog
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import org.jetbrains.compose.resources.painterResource

private const val EXTENSION_SEPARATOR = '.'

@Composable
fun FilesMenu(viewModel: ScoreFilesViewModel) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var exporting by remember { mutableStateOf<Attachment?>(null) }
    var deleting by remember { mutableStateOf<Attachment?>(null) }
    val picker = rememberFilePickerLauncher(FileKitType.File(), FileKitMode.Multiple()) { files -> files?.let(viewModel::add) }
    val saver = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { target ->
        val attachment = exporting
        exporting = null
        if (target != null && attachment != null) viewModel.export(attachment, target)
    }
    val export = { attachment: Attachment ->
        expanded = false
        exporting = attachment
        val extension = attachment.name.substringAfterLast(EXTENSION_SEPARATOR, "").ifEmpty { null }
        saver.launch(suggestedName = attachment.name.substringBeforeLast(EXTENSION_SEPARATOR), defaultExtension = extension)
    }

    Box {
        IconAction(Res.drawable.ic_attach_file, strings.attachments, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (state.files.isEmpty()) {
                Text(
                    strings.noAttachments,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                )
            }
            state.files.forEach { attachment ->
                DropdownMenuItem(
                    text = { Text(attachment.name, maxLines = 1, overflow = TextOverflow.MiddleEllipsis) },
                    onClick = { export(attachment) },
                    leadingIcon = { Icon(painterResource(Res.drawable.ic_description), contentDescription = null) },
                    trailingIcon = {
                        Row {
                            IconAction(Res.drawable.ic_download, strings.saveAs, onClick = { export(attachment) })
                            IconAction(Res.drawable.ic_delete, strings.delete, onClick = {
                                expanded = false
                                deleting = attachment
                            })
                        }
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(strings.attachFile) },
                onClick = {
                    expanded = false
                    picker.launch()
                },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_add), contentDescription = null) },
            )
        }
    }

    deleting?.let { attachment ->
        ConfirmDeleteDialog(
            text = strings.deleteAttachmentText(attachment.name),
            onConfirm = {
                viewModel.delete(attachment)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
    if (state.failed) MessageDialog(strings.attachments, strings.attachmentFailed, viewModel::dismissFailure)
}
