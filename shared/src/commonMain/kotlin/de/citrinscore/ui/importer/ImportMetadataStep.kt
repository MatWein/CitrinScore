package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.CitrinTextField
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.components.text
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing

@Composable
fun ImportMetadataStep(
    state: ImportState,
    onTitleChange: (String) -> Unit,
    onComposerChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val titleError = state.titleError?.takeIf { state.title.isNotEmpty() }?.text
    val fieldModifier = Modifier.fillMaxWidth()

    WizardStep(strings.importMetadataTitle, strings.importMetadataSubtitle) {
        Column(Modifier.widthIn(max = ContentWidth.Form).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
            CitrinTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = strings.titleLabel,
                singleLine = true,
                error = titleError,
                modifier = fieldModifier,
            )
            CitrinTextField(
                value = state.composer,
                onValueChange = onComposerChange,
                label = strings.composerLabel,
                singleLine = true,
                modifier = fieldModifier,
            )
            CitrinTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = strings.descriptionLabel,
                minLines = 3,
                modifier = fieldModifier,
            )
            if (state.saveFailed) {
                Text(strings.importFailed, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
