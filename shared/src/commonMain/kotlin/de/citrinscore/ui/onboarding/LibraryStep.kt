package de.citrinscore.ui.onboarding

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.settings.LibraryFolderTile
import de.citrinscore.ui.theme.ContentWidth
import io.github.vinceglb.filekit.PlatformFile

@Composable
fun LibraryStep(
    library: PlatformFile?,
    showError: Boolean,
    onSelect: (PlatformFile) -> Unit,
    onError: () -> Unit,
) {
    val strings = LocalStrings.current
    WizardStep(strings.libraryLocationTitle, strings.libraryLocationSubtitle) {
        LibraryFolderTile(library, showError, onSelect, onError, Modifier.widthIn(max = ContentWidth.Form).fillMaxWidth())
    }
}
