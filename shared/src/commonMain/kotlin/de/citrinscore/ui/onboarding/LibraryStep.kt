package de.citrinscore.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.StepHeader
import de.citrinscore.ui.settings.LibraryFolderTile
import io.github.vinceglb.filekit.PlatformFile

@Composable
fun LibraryStep(
    library: PlatformFile?,
    showError: Boolean,
    onSelect: (PlatformFile) -> Unit,
    onError: () -> Unit,
) {
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.libraryLocationTitle, strings.libraryLocationSubtitle)
        LibraryFolderTile(library, showError, onSelect, onError, Modifier.widthIn(max = 640.dp).fillMaxWidth())
    }
}
