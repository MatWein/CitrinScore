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
fun OmrSection(enabled: Boolean, display: OmrDisplay, onEnabledChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        Row(
            modifier = Modifier.fillMaxWidth().toggleable(value = enabled, role = Role.Checkbox, onValueChange = onEnabledChange),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Checkbox(checked = enabled, onCheckedChange = null)
            Text(strings.omrCreatePart, style = MaterialTheme.typography.bodyLarge)
        }
        when (display) {
            OmrDisplay.Idle -> Unit
            is OmrDisplay.Recognized -> PagePreview(display.preview, isLoading = false, failed = false, label = strings.omrPreviewLabel)
            OmrDisplay.Failed -> Text(strings.omrFailed, color = MaterialTheme.colorScheme.error)
            is OmrDisplay.Recognizing -> Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                Text(strings.omrRecognizing, style = MaterialTheme.typography.bodyMedium)
                val progress = display.progress
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}
