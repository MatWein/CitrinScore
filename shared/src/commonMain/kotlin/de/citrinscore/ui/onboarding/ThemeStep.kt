package de.citrinscore.ui.onboarding

import androidx.compose.runtime.Composable
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.settings.ThemeMode
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.settings.ThemeOptions

@Composable
fun ThemeStep(selected: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    val strings = LocalStrings.current
    WizardStep(strings.themeTitle, strings.themeSubtitle) {
        ThemeOptions(selected, onSelect)
    }
}
