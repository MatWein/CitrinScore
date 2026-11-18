package de.citrinscore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.settings.GitRemote
import de.citrinscore.sync.LibrarySync
import de.citrinscore.sync.SyncException
import de.citrinscore.sync.SyncFailure
import de.citrinscore.ui.components.CitrinTextField
import de.citrinscore.ui.sync.message
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.launch

private val ProgressSize = 20.dp

@Composable
fun SyncSettings(remote: GitRemote?, library: PlatformFile, sync: LibrarySync, available: Boolean, onRemoteChange: (GitRemote?) -> Unit) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(remote?.url.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(remote?.username.orEmpty()) }
    var token by rememberSaveable { mutableStateOf(remote?.token.orEmpty()) }
    var connecting by rememberSaveable { mutableStateOf(false) }
    var failure by rememberSaveable { mutableStateOf<SyncFailure?>(null) }
    var detail by rememberSaveable { mutableStateOf<String?>(null) }
    val candidate = GitRemote(url.trim(), token.trim(), username.trim())
    val connect = {
        failure = null
        detail = null
        connecting = true
        scope.launch {
            try {
                sync.connect(library, candidate)
                onRemoteChange(candidate)
            } catch (e: SyncException) {
                failure = e.failure
                detail = e.message
            }
            connecting = false
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text(strings.syncSettingsHint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!available) {
            Text(strings.syncLibraryLocation, style = MaterialTheme.typography.bodyLarge)
            if (remote != null) OutlinedButton(onClick = { onRemoteChange(null) }) { Text(strings.disconnect) }
            return@Column
        }
        if (remote != null) {
            Text(strings.syncConnectedTo(remote.url), style = MaterialTheme.typography.bodyLarge)
            OutlinedButton(onClick = { onRemoteChange(null) }) { Text(strings.disconnect) }
            return@Column
        }
        val editable = !connecting
        CitrinTextField(
            url,
            { url = it },
            strings.gitUrl,
            Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = editable,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
        )
        CitrinTextField(username, { username = it }, strings.gitUsername, Modifier.fillMaxWidth(), singleLine = true, enabled = editable)
        CitrinTextField(
            token,
            { token = it },
            strings.gitToken,
            Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = editable,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        )
        failure?.let { reason ->
            Text(strings.message(reason), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Button(
                onClick = { connect() },
                enabled = editable && candidate.url.isNotEmpty() && candidate.token.isNotEmpty(),
            ) { Text(strings.connect) }
            if (connecting) CircularProgressIndicator(Modifier.size(ProgressSize))
        }
    }
}
