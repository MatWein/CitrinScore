package de.citrinscore.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.DialogProperties
import de.citrinscore.i18n.LocalStrings

private val Blocking = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)

@Composable
fun MoveLibraryDialog(target: String, stopsSync: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.moveLibraryTitle) },
        text = { Text(strings.moveLibraryText(target, stopsSync)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(strings.moveLibrary) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } },
    )
}

@Composable
fun MovingLibraryDialog() {
    AlertDialog(
        onDismissRequest = {},
        properties = Blocking,
        title = { Text(LocalStrings.current.movingLibrary) },
        text = { LinearProgressIndicator(Modifier.fillMaxWidth()) },
        confirmButton = {},
    )
}
