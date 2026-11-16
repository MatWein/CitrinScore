package de.citrinscore.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.AppLanguage
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.StepHeader
import de.citrinscore.ui.settings.LanguageOptions

@Composable
fun LanguageStep(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.languageTitle, strings.languageSubtitle)
        LanguageOptions(selected, onSelect)
    }
}
