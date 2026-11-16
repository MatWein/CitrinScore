package de.citrinscore.ui.viewer.audio

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_mic
import de.citrinscore.ui.components.CitrinTextField
import de.citrinscore.ui.theme.Spacing
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.jetbrains.compose.resources.painterResource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val ClockInterval = 200.milliseconds

@Composable
fun RecordingDialog(start: TimeSource.Monotonic.ValueTimeMark, onSave: () -> Unit, onDiscard: () -> Unit) {
    val strings = LocalStrings.current
    var elapsed by remember(start) { mutableStateOf(Duration.ZERO) }
    LaunchedEffect(start) {
        while (isActive) {
            elapsed = start.elapsedNow()
            delay(ClockInterval)
        }
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(strings.recordingTitle) },
        text = {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Icon(painterResource(Res.drawable.ic_mic), contentDescription = null, tint = MaterialTheme.colorScheme.error)
                Text(elapsed.clock(), style = MaterialTheme.typography.headlineMedium)
            }
        },
        confirmButton = { TextButton(onClick = onSave) { Text(strings.saveRecording) } },
        dismissButton = { TextButton(onClick = onDiscard) { Text(strings.discardRecording) } },
    )
}

@Composable
fun RenameAudioDialog(initialName: String, onRename: (String) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    var name by rememberSaveable { mutableStateOf(initialName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.renameTitle) },
        text = { CitrinTextField(value = name, onValueChange = { name = it }, label = strings.nameLabel, singleLine = true) },
        confirmButton = { TextButton(onClick = { onRename(name.trim()) }, enabled = name.isNotBlank()) { Text(strings.rename) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
    )
}

@Composable
fun AudioErrorDialog(error: AudioError, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.audioFailedTitle) },
        text = {
            Text(
                when (error) {
                    AudioError.Playback -> strings.audioPlaybackFailed
                    AudioError.Recording -> strings.audioRecordingFailed
                    AudioError.Save -> strings.audioSaveFailed
                },
            )
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(strings.ok) } },
    )
}

internal fun Duration.clock(): String = toComponents { minutes, seconds, _ -> "$minutes:${seconds.toString().padStart(2, '0')}" }
