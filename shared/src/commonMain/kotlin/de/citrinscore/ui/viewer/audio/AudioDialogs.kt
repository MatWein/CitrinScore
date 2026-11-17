package de.citrinscore.ui.viewer.audio

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.MessageDialog
import de.citrinscore.ui.components.TextInputDialog
import kotlin.time.Duration

@Composable
fun RecordingFinishedDialog(length: Duration, onSave: () -> Unit, onDiscard: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = {},
        title = { Text(strings.recordingTitle) },
        text = { Text(strings.recordingLength(length.clock())) },
        confirmButton = { TextButton(onClick = onSave) { Text(strings.saveRecording) } },
        dismissButton = { TextButton(onClick = onDiscard) { Text(strings.discardRecording) } },
    )
}

@Composable
fun RenameAudioDialog(initialName: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    TextInputDialog(
        title = strings.renameTitle,
        label = strings.nameLabel,
        initialText = initialName,
        confirmLabel = strings.rename,
        allowBlank = false,
        onConfirm = onRename,
        onDismiss = onDismiss,
    )
}

@Composable
fun AudioErrorDialog(error: AudioError, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val message = when (error) {
        AudioError.Playback -> strings.audioPlaybackFailed
        AudioError.Recording -> strings.audioRecordingFailed
        AudioError.Save -> strings.audioSaveFailed
    }
    MessageDialog(strings.audioFailedTitle, message, onDismiss)
}

internal fun Duration.clock(): String = toComponents { minutes, seconds, _ -> "$minutes:${seconds.toString().padStart(2, '0')}" }
