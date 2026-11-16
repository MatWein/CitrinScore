package de.citrinscore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.library.displayPath
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_folder
import de.citrinscore.ui.components.IconBadge
import de.citrinscore.ui.components.IconBadgeSize
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.name

@Composable
fun LibraryFolderTile(
    library: PlatformFile?,
    showError: Boolean,
    onSelect: (PlatformFile) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    val picker = rememberDirectoryPickerLauncher(directory = library, onError = { onError() }) { directory ->
        directory?.let(onSelect)
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        Tile(Modifier.fillMaxWidth(), selected = library != null, onClick = picker::launch) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Page)) {
                IconBadge(Res.drawable.ic_folder, size = IconBadgeSize.Large)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    Text(
                        library?.name ?: strings.chooseFolder,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    library?.displayPath?.let { path ->
                        Text(
                            path,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.MiddleEllipsis,
                        )
                    }
                }
                if (library != null) {
                    Text(strings.changeFolder, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
        if (showError) {
            Text(strings.folderPickerFailed, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
