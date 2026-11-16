package de.citrinscore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import de.citrinscore.ui.theme.Spacing

@Composable
fun WizardStep(
    title: String,
    subtitle: String,
    leading: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Section)) {
        leading()
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            Text(title, style = MaterialTheme.typography.displaySmall, textAlign = TextAlign.Center)
            Text(
                subtitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        content()
    }
}
