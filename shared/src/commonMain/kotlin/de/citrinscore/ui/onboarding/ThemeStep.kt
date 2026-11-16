package de.citrinscore.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.settings.ThemeMode
import de.citrinscore.ui.components.StepHeader
import de.citrinscore.ui.settings.ThemeOptions

@Composable
fun ThemeStep(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.themeTitle, strings.themeSubtitle)
        ThemeOptions(selected, onSelect)
    }
}
