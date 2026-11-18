package de.citrinscore.ui.sync

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.sync.ConflictChoice
import de.citrinscore.sync.SyncConflict
import de.citrinscore.sync.SyncFailure
import de.citrinscore.sync.SyncOutcome
import de.citrinscore.sync.SyncStep
import de.citrinscore.ui.theme.Spacing

private val ConflictListHeight = 360.dp
private val Blocking = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)

@Composable
fun SyncDialog(state: SyncState, viewModel: SyncViewModel) {
    val strings = LocalStrings.current
    when (state) {
        SyncState.Idle -> Unit
        is SyncState.Running -> AlertDialog(
            onDismissRequest = {},
            properties = Blocking,
            title = { Text(strings.sync) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                    Text(strings.label(state.progress.step))
                    val fraction = state.progress.fraction
                    if (fraction == null) LinearProgressIndicator(Modifier.fillMaxWidth()) else LinearProgressIndicator({ fraction }, Modifier.fillMaxWidth())
                }
            },
            confirmButton = {},
        )
        is SyncState.Resolving -> ConflictDialog(state, viewModel)
        is SyncState.Finished -> ResultDialog(strings.sync, strings.message(state.outcome), null, viewModel::dismiss)
        is SyncState.Failed -> ResultDialog(strings.sync, strings.message(state.failure), state.detail, viewModel::dismiss)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConflictDialog(state: SyncState.Resolving, viewModel: SyncViewModel) {
    val strings = LocalStrings.current
    AlertDialog(
        onDismissRequest = {},
        properties = Blocking,
        title = { Text(strings.syncConflictsTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                Text(strings.syncConflictsText)
                LazyColumn(Modifier.heightIn(max = ConflictListHeight), verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                    items(state.conflicts, key = SyncConflict::path) { conflict ->
                        Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                            Text(conflict.title, style = MaterialTheme.typography.titleSmall)
                            if (conflict.folder.isNotEmpty()) {
                                Text(conflict.folder, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                                ConflictChoice.entries.forEachIndexed { index, choice ->
                                    SegmentedButton(
                                        selected = state.choices[conflict.path] == choice,
                                        onClick = { viewModel.choose(conflict, choice) },
                                        shape = SegmentedButtonDefaults.itemShape(index, ConflictChoice.entries.size),
                                    ) { Text(strings.label(choice)) }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = viewModel::resume, enabled = state.isComplete) { Text(strings.continueSync) } },
        dismissButton = { TextButton(onClick = viewModel::cancel) { Text(strings.cancelSync) } },
    )
}

@Composable
private fun ResultDialog(title: String, message: String, detail: String?, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(message)
                detail?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(LocalStrings.current.ok) } },
    )
}

private fun Strings.label(step: SyncStep) = when (step) {
    SyncStep.Committing -> syncCommitting
    SyncStep.Downloading -> syncDownloading
    SyncStep.Merging -> syncMerging
    SyncStep.Uploading -> syncUploading
}

private fun Strings.label(choice: ConflictChoice) = when (choice) {
    ConflictChoice.Local -> keepLocal
    ConflictChoice.Remote -> keepRemote
}

private fun Strings.message(outcome: SyncOutcome) = when (outcome) {
    SyncOutcome.Cancelled -> syncCancelled
    is SyncOutcome.Completed ->
        if (outcome.uploaded == 0 && outcome.downloaded == 0) syncUpToDate else syncCompleted(outcome.uploaded, outcome.downloaded)
}

fun Strings.message(failure: SyncFailure) = when (failure) {
    SyncFailure.LibraryLocation -> syncLibraryLocation
    SyncFailure.Connection -> syncConnection
    SyncFailure.Rejected -> syncRejected
    SyncFailure.Failed -> syncFailed
}
