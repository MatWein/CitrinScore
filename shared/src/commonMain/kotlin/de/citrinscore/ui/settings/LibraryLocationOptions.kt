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
import de.citrinscore.resources.ic_library_music
import de.citrinscore.settings.LibraryTarget
import de.citrinscore.settings.appStorageSelectable
import de.citrinscore.ui.components.IconBadge
import de.citrinscore.ui.components.IconBadgeSize
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.dialogs.compose.rememberDirectoryPickerLauncher
import io.github.vinceglb.filekit.name
import org.jetbrains.compose.resources.DrawableResource

@Composable
fun LibraryLocationOptions(
    selected: LibraryTarget?,
    showError: Boolean,
    onSelect: (LibraryTarget) -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val strings = LocalStrings.current
    val folder = (selected as? LibraryTarget.Folder)?.directory
    val picker = rememberDirectoryPickerLauncher(directory = folder, onError = { onError() }) { directory ->
        directory?.let { onSelect(LibraryTarget.Folder(it)) }
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        if (appStorageSelectable) {
            LocationTile(
                icon = Res.drawable.ic_library_music,
                title = strings.libraryInApp,
                description = strings.libraryInAppText,
                selected = selected == LibraryTarget.App,
                onClick = { onSelect(LibraryTarget.App) }.takeIf { enabled },
            )
        }
        LocationTile(
            icon = Res.drawable.ic_folder,
            title = folder?.name ?: if (appStorageSelectable) strings.libraryOwnFolder else strings.chooseFolder,
            description = strings.libraryOwnFolderText.takeIf { appStorageSelectable },
            detail = folder?.displayPath,
            action = when {
                folder != null -> strings.changeFolder
                appStorageSelectable -> strings.chooseFolder
                else -> null
            },
            selected = folder != null,
            onClick = { picker.launch() }.takeIf { enabled },
        )
        if (showError) {
            Text(strings.folderPickerFailed, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun LocationTile(
    icon: DrawableResource,
    title: String,
    description: String?,
    selected: Boolean,
    onClick: (() -> Unit)?,
    detail: String? = null,
    action: String? = null,
) {
    Tile(Modifier.fillMaxWidth(), selected = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Page)) {
            IconBadge(icon, size = IconBadgeSize.Large)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                Text(title, style = MaterialTheme.typography.headlineSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                detail?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.MiddleEllipsis,
                    )
                }
                description?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                action?.let { Text(it, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary) }
            }
        }
    }
}
