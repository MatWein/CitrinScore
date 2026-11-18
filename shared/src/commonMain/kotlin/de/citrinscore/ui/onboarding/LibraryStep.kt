package de.citrinscore.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.settings.LibraryTarget
import de.citrinscore.ui.settings.LibraryLocationOptions
import de.citrinscore.ui.theme.ContentWidth

@Composable
fun LibraryStep(
    library: LibraryTarget?,
    showError: Boolean,
    onSelect: (LibraryTarget) -> Unit,
    onError: () -> Unit,
) {
    val strings = LocalStrings.current
    WizardStep(strings.libraryLocationTitle, strings.libraryLocationSubtitle) {
        LibraryLocationOptions(library, showError, onSelect, onError, Modifier.widthIn(max = ContentWidth.Form).fillMaxWidth())
    }
}
