package de.citrinscore.ui.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.library.NameError
import de.citrinscore.ui.components.CitrinTextField
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.components.text
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing

private const val DESCRIPTION_MIN_LINES = 3

class ScoreDetails(val title: String, val composer: String, val description: String, val titleError: NameError?)

@Composable
fun ScoreDetailsStep(
    stepTitle: String,
    stepSubtitle: String,
    details: ScoreDetails,
    failure: String?,
    onTitleChange: (String) -> Unit,
    onComposerChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
) {
    val strings = LocalStrings.current
    val fieldModifier = Modifier.fillMaxWidth()

    WizardStep(stepTitle, stepSubtitle) {
        Column(Modifier.widthIn(max = ContentWidth.Form).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
            CitrinTextField(
                value = details.title,
                onValueChange = onTitleChange,
                label = strings.titleLabel,
                singleLine = true,
                error = details.titleError?.takeIf { details.title.isNotEmpty() }?.text,
                modifier = fieldModifier,
            )
            CitrinTextField(
                value = details.composer,
                onValueChange = onComposerChange,
                label = strings.composerLabel,
                singleLine = true,
                modifier = fieldModifier,
            )
            CitrinTextField(
                value = details.description,
                onValueChange = onDescriptionChange,
                label = strings.descriptionLabel,
                minLines = DESCRIPTION_MIN_LINES,
                modifier = fieldModifier,
            )
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
