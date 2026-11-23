package de.citrinscore.ui.library

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.library.validateName
import de.citrinscore.ui.components.CitrinTextField
import de.citrinscore.ui.components.initialFocus
import de.citrinscore.ui.components.text

@Composable
fun NewFolderDialog(takenNames: List<String>, onCreate: (String) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    var name by rememberSaveable { mutableStateOf("") }
    val error = validateName(name, takenNames)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.newFolderTitle) },
        text = {
            CitrinTextField(
                value = name,
                onValueChange = { name = it },
                label = strings.folderNameLabel,
                singleLine = true,
                error = error?.takeIf { name.isNotEmpty() }?.text,
                modifier = Modifier.initialFocus(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onCreate(name) }, enabled = error == null) { Text(strings.create) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(strings.cancel) }
        },
    )
}
