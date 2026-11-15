package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.StepHeader
import de.citrinscore.ui.components.text

@Composable
fun ImportMetadataStep(
    state: ImportState,
    onTitleChange: (String) -> Unit,
    onComposerChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val titleError = state.titleError?.takeIf { state.title.isNotEmpty() }
    val fieldModifier = Modifier.fillMaxWidth()

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.importMetadataTitle, strings.importMetadataSubtitle)
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(strings.titleLabel) },
                singleLine = true,
                isError = titleError != null,
                supportingText = titleError?.let { { Text(it.text) } },
                modifier = fieldModifier,
            )
            OutlinedTextField(
                value = state.composer,
                onValueChange = onComposerChange,
                label = { Text(strings.composerLabel) },
                singleLine = true,
                modifier = fieldModifier,
            )
            OutlinedTextField(
                value = state.description,
                onValueChange = onDescriptionChange,
                label = { Text(strings.descriptionLabel) },
                minLines = 3,
                modifier = fieldModifier,
            )
            if (state.saveFailed) {
                Text(strings.importFailed, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
