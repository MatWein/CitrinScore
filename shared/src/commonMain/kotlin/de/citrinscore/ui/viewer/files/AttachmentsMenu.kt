package de.citrinscore.ui.viewer.files

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Badge
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
import de.citrinscore.resources.ic_attach_file
import de.citrinscore.resources.ic_delete
import de.citrinscore.resources.ic_description
import de.citrinscore.resources.ic_download
import de.citrinscore.resources.ic_edit
import de.citrinscore.resources.ic_mic
import de.citrinscore.resources.ic_play
import de.citrinscore.resources.ic_stop
import de.citrinscore.score.Attachment
import de.citrinscore.score.AttachmentKind
import de.citrinscore.score.CitScore
import de.citrinscore.ui.components.ConfirmDeleteDialog
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.MessageDialog
import de.citrinscore.ui.theme.Spacing
import de.citrinscore.ui.viewer.audio.AudioErrorDialog
import de.citrinscore.ui.viewer.audio.RecordingFinishedDialog
import de.citrinscore.ui.viewer.audio.RenameAudioDialog
import de.citrinscore.ui.viewer.audio.ScoreAudioViewModel
import de.citrinscore.ui.viewer.audio.rememberMicrophoneAccess
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.extension
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private class PendingDeletion(val kind: AttachmentKind, val attachment: Attachment)

@Composable
fun AttachmentsMenu(audioViewModel: ScoreAudioViewModel, filesViewModel: ScoreFilesViewModel, onOpenBar: () -> Unit) {
    val strings = LocalStrings.current
    val audio by audioViewModel.state.collectAsStateWithLifecycle()
    val files by filesViewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Attachment?>(null) }
    var deleting by remember { mutableStateOf<PendingDeletion?>(null) }
    val saveAudio = rememberAttachmentSaver(audioViewModel::export)
    val saveFile = rememberAttachmentSaver(filesViewModel::export)
    val picker = rememberFilePickerLauncher(FileKitType.File(), FileKitMode.Multiple()) { picked ->
        val (audioFiles, otherFiles) = picked.orEmpty().partition { it.extension.lowercase() in CitScore.AUDIO_EXTENSIONS }
        if (audioFiles.isNotEmpty()) audioViewModel.addFiles(audioFiles)
        if (otherFiles.isNotEmpty()) filesViewModel.add(otherFiles)
    }
    val requestRecording = rememberMicrophoneAccess {
        onOpenBar()
        audioViewModel.startRecording()
    }
    val close = { action: () -> Unit ->
        expanded = false
        action()
    }
    LaunchedEffect(expanded) { if (expanded) audioViewModel.checkMicrophone() }

    Box {
        val count = audio.attachments.size + files.files.size
        IconAction(
            Res.drawable.ic_attach_file,
            strings.attachments,
            onClick = { expanded = true },
            badge = {
                if (count > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) { Text(count.toString()) }
                }
            },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (audio.attachments.isEmpty() && files.files.isEmpty()) {
                Text(
                    strings.noAttachments,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
                )
            }
            audio.attachments.forEach { attachment ->
                DropdownMenuItem(
                    text = { AttachmentName(attachment.name) },
                    onClick = {
                        close {
                            onOpenBar()
                            audioViewModel.load(attachment)
                        }
                    },
                    enabled = !audio.isRecording,
                    leadingIcon = { MenuIcon(Res.drawable.ic_play) },
                    trailingIcon = {
                        Row {
                            IconAction(Res.drawable.ic_edit, strings.rename, onClick = { close { renaming = attachment } })
                            IconAction(Res.drawable.ic_download, strings.saveAs, onClick = { close { saveAudio(attachment) } })
                            IconAction(Res.drawable.ic_delete, strings.delete, onClick = { close { deleting = PendingDeletion(AttachmentKind.Audio, attachment) } })
                        }
                    },
                )
            }
            files.files.forEach { attachment ->
                DropdownMenuItem(
                    text = { AttachmentName(attachment.name) },
                    onClick = { close { saveFile(attachment) } },
                    leadingIcon = { MenuIcon(Res.drawable.ic_description) },
                    trailingIcon = {
                        Row {
                            IconAction(Res.drawable.ic_download, strings.saveAs, onClick = { close { saveFile(attachment) } })
                            IconAction(Res.drawable.ic_delete, strings.delete, onClick = { close { deleting = PendingDeletion(AttachmentKind.File, attachment) } })
                        }
                    },
                )
            }
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(strings.attachFile) },
                onClick = { close { picker.launch() } },
                leadingIcon = { MenuIcon(Res.drawable.ic_add) },
            )
            if (audio.isRecording) {
                DropdownMenuItem(
                    text = { Text(strings.stopRecording) },
                    onClick = { close(audioViewModel::stopRecording) },
                    leadingIcon = { MenuIcon(Res.drawable.ic_stop) },
                )
            } else {
                DropdownMenuItem(
                    text = { Text(strings.startRecording) },
                    onClick = { close(requestRecording) },
                    leadingIcon = { MenuIcon(Res.drawable.ic_mic) },
                    enabled = audio.microphoneAvailable == true,
                )
            }
        }
    }

    audio.finishedRecording?.let { length ->
        RecordingFinishedDialog(
            length = length,
            onSave = { audioViewModel.saveRecording(strings.recordingName(audio.nextRecordingNumber)) },
            onDiscard = audioViewModel::discardRecording,
        )
    }
    renaming?.let { attachment ->
        RenameAudioDialog(
            initialName = attachment.name,
            onRename = { name ->
                audioViewModel.rename(attachment, name)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { pending ->
        ConfirmDeleteDialog(
            text = strings.deleteAttachmentText(pending.attachment.name),
            onConfirm = {
                when (pending.kind) {
                    AttachmentKind.Audio -> audioViewModel.delete(pending.attachment)
                    AttachmentKind.File -> filesViewModel.delete(pending.attachment)
                }
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
    audio.error?.let { AudioErrorDialog(it, audioViewModel::dismissError) }
    if (files.failed) MessageDialog(strings.attachments, strings.attachmentFailed, filesViewModel::dismissFailure)
}

@Composable
private fun MenuIcon(icon: DrawableResource) = Icon(painterResource(icon), contentDescription = null)
