package de.citrinscore.ui.viewer.audio

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_add
import de.citrinscore.resources.ic_delete
import de.citrinscore.resources.ic_download
import de.citrinscore.resources.ic_edit
import de.citrinscore.resources.ic_headphones
import de.citrinscore.resources.ic_mic
import de.citrinscore.resources.ic_play
import de.citrinscore.resources.ic_stop
import de.citrinscore.score.Attachment
import de.citrinscore.score.CitScore
import de.citrinscore.ui.components.ConfirmDeleteDialog
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.theme.Spacing
import de.citrinscore.ui.viewer.files.AttachmentName
import de.citrinscore.ui.viewer.files.rememberAttachmentSaver
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AudioMenu(viewModel: ScoreAudioViewModel, onOpenBar: () -> Unit) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Attachment?>(null) }
    var deleting by remember { mutableStateOf<Attachment?>(null) }
    val save = rememberAttachmentSaver(viewModel::export)
    val picker = rememberFilePickerLauncher(FileKitType.File(CitScore.AUDIO_EXTENSIONS), FileKitMode.Multiple()) { files ->
        files?.let(viewModel::addFiles)
    }
    val requestRecording = rememberMicrophoneAccess {
        onOpenBar()
        viewModel.startRecording()
    }
    LaunchedEffect(expanded) { if (expanded) viewModel.checkMicrophone() }

    Box {
        IconAction(Res.drawable.ic_headphones, strings.audio, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (state.attachments.isEmpty()) {
                Text(
                    strings.noAudio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                )
            }
            state.attachments.forEach { attachment ->
                DropdownMenuItem(
                    text = { AttachmentName(attachment.name) },
                    onClick = {
                        expanded = false
                        onOpenBar()
                        viewModel.load(attachment)
                    },
                    enabled = !state.isRecording,
                    leadingIcon = { MenuIcon(Res.drawable.ic_play) },
                    trailingIcon = {
                        Row {
                            IconAction(Res.drawable.ic_edit, strings.rename, onClick = {
                                expanded = false
                                renaming = attachment
                            })
                            IconAction(Res.drawable.ic_download, strings.saveAs, onClick = {
                                expanded = false
                                save(attachment)
                            })
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
                text = { Text(strings.addAudioFile) },
                onClick = {
                    expanded = false
                    picker.launch()
                },
                leadingIcon = { MenuIcon(Res.drawable.ic_add) },
            )
            if (state.isRecording) {
                DropdownMenuItem(
                    text = { Text(strings.stopRecording) },
                    onClick = {
                        expanded = false
                        viewModel.stopRecording()
                    },
                    leadingIcon = { MenuIcon(Res.drawable.ic_stop) },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(strings.startRecording) },
                    onClick = {
                        expanded = false
                        requestRecording()
                    },
                    leadingIcon = { MenuIcon(Res.drawable.ic_mic) },
                    enabled = state.microphoneAvailable == true,
                )
            }
        }
    }

    state.finishedRecording?.let { length ->
        RecordingFinishedDialog(
            length = length,
            onSave = { viewModel.saveRecording(strings.recordingName(state.nextRecordingNumber)) },
            onDiscard = viewModel::discardRecording,
        )
    }
    renaming?.let { attachment ->
        RenameAudioDialog(
            initialName = attachment.name,
            onRename = { name ->
                viewModel.rename(attachment, name)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
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
    state.error?.let { AudioErrorDialog(it, viewModel::dismissError) }
}

@Composable
private fun MenuIcon(icon: DrawableResource) = Icon(painterResource(icon), contentDescription = null)
