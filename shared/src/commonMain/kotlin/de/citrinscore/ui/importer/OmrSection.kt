package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.pages.PagePreview
import de.citrinscore.ui.theme.Spacing

@Composable
fun OmrSection(draft: PartDraft, editor: PartDraftEditor, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        Row(
            modifier = Modifier.fillMaxWidth().toggleable(value = draft.omrEnabled, role = Role.Checkbox, onValueChange = editor::enableOmr),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Checkbox(checked = draft.omrEnabled, onCheckedChange = null)
            Text(strings.omrCreatePart, style = MaterialTheme.typography.bodyLarge)
        }
        if (!draft.isOmrRequested) return@Column
        val recognized = draft.recognized
        val omr = draft.omr
        when {
            recognized != null -> PagePreview(recognized.preview, isLoading = false, failed = false, label = strings.omrPreviewLabel)
            omr is OmrState.Failed && omr.input == draft.omrInput -> Text(strings.omrFailed, color = MaterialTheme.colorScheme.error)
            draft.isPreviewReady -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(strings.omrRecognizing, style = MaterialTheme.typography.bodyMedium)
                val running = omr as? OmrState.Running
                if (running != null && running.input == draft.omrInput) {
                    LinearProgressIndicator(progress = { running.progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
