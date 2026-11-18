package de.citrinscore.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_folder
import de.citrinscore.settings.LibraryLocation
import de.citrinscore.ui.components.IconBadge
import de.citrinscore.ui.components.IconBadgeSize
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher

@Composable
fun LibraryUnavailableScreen(
    location: LibraryLocation,
    onRetry: (onFailure: () -> Unit) -> Unit,
    onChooseFolder: (directory: PlatformFile, onFailure: () -> Unit) -> Unit,
) {
    val strings = LocalStrings.current
    var error by remember { mutableStateOf<String?>(null) }
    val picker = rememberDirectoryPickerLauncher(onError = { error = strings.folderPickerFailed }) { directory ->
        directory?.let { onChooseFolder(it) { error = strings.folderPickerFailed } }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(Spacing.Page), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.widthIn(max = ContentWidth.Form),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
        ) {
            IconBadge(Res.drawable.ic_folder, size = IconBadgeSize.Hero)
            Text(strings.libraryUnavailableTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text(
                strings.libraryUnavailableText(
                    when (location) {
                        LibraryLocation.App -> strings.libraryInApp
                        is LibraryLocation.Folder -> location.displayPath
                    },
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = {
                error = null
                onRetry { error = strings.libraryStillUnavailable }
            }) { Text(strings.retry) }
            OutlinedButton(onClick = picker::launch) { Text(strings.chooseOtherFolder) }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
        }
    }
}
