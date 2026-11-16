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
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_add
import de.citrinscore.resources.ic_delete
import de.citrinscore.resources.ic_edit
import de.citrinscore.resources.ic_headphones
import de.citrinscore.resources.ic_mic
import de.citrinscore.resources.ic_play
import de.citrinscore.score.AudioAttachment
import de.citrinscore.score.CitScore
import de.citrinscore.ui.components.ConfirmDeleteDialog
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun AudioMenu(viewModel: ScoreAudioViewModel) {
    val strings = LocalStrings.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var expanded by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<AudioAttachment?>(null) }
    var deleting by remember { mutableStateOf<AudioAttachment?>(null) }
    val picker = rememberFilePickerLauncher(FileKitType.File(listOf(CitScore.AUDIO_EXTENSION)), FileKitMode.Multiple()) { files ->
        files?.let(viewModel::addFiles)
    }
    val requestRecording = rememberMicrophoneAccess(viewModel::startRecording)
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
                    text = { Text(attachment.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        expanded = false
                        viewModel.load(attachment)
                    },
                    leadingIcon = { MenuIcon(Res.drawable.ic_play) },
                    trailingIcon = {
                        Row {
                            IconAction(Res.drawable.ic_edit, strings.rename, onClick = {
                                expanded = false
                                renaming = attachment
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
            DropdownMenuItem(
                text = { Text(strings.recordAudio) },
                onClick = {
                    expanded = false
                    requestRecording()
                },
                leadingIcon = { MenuIcon(Res.drawable.ic_mic) },
                enabled = state.microphoneAvailable == true,
            )
        }
    }

    state.recordingStart?.let { start ->
        RecordingDialog(
            start = start,
            onSave = { viewModel.finishRecording(strings.recordingName(state.attachments.size + 1)) },
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
            text = strings.deleteAudioText(attachment.name),
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
