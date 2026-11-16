package de.citrinscore.ui.onboarding

import androidx.compose.runtime.Composable
import de.citrinscore.i18n.AppLanguage
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.settings.LanguageOptions

@Composable
fun LanguageStep(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val strings = LocalStrings.current
    WizardStep(strings.languageTitle, strings.languageSubtitle) {
        LanguageOptions(selected, onSelect)
    }
}
