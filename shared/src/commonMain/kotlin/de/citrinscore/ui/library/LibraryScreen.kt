package de.citrinscore.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.library.FolderContent
import de.citrinscore.library.LibraryItem
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_folder
import de.citrinscore.resources.ic_library_music
import de.citrinscore.resources.ic_music_note
import de.citrinscore.ui.components.IconBadge
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.components.TileGrid
import de.citrinscore.ui.components.VerticalScrollIndicator
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name

@Composable
fun LibraryScreen(viewModel: LibraryViewModel, onImport: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        LibraryToolbar(
            folderName = state.currentFolder.name.takeUnless { state.isRoot },
            onUp = viewModel::up,
            onNewFolder = { showNewFolderDialog = true },
            onImport = onImport,
            modifier = Modifier.fillMaxWidth(),
        )
        Box(Modifier.weight(1f).fillMaxWidth()) {
            state.content?.let { content ->
                if (content.isEmpty) EmptyLibrary(Modifier.align(Alignment.Center)) else LibraryGrid(content, viewModel::open)
            }
        }
    }

    val content = state.content
    if (showNewFolderDialog && content != null) {
        NewFolderDialog(
            takenNames = content.names,
            onCreate = { name ->
                viewModel.createFolder(name)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false },
        )
    }
}

@Composable
private fun LibraryGrid(content: FolderContent, onOpenFolder: (PlatformFile) -> Unit) {
    val scrollState = rememberScrollState()
    Box(Modifier.fillMaxSize()) {
        TileGrid(
            items = content.items,
            minTileWidth = 200.dp,
            modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(end = 12.dp),
        ) { item, modifier ->
            LibraryTile(item, onOpenFolder, modifier)
        }
        VerticalScrollIndicator(scrollState, Modifier.align(Alignment.CenterEnd).fillMaxHeight())
    }
}

@Composable
private fun LibraryTile(item: LibraryItem, onOpenFolder: (PlatformFile) -> Unit, modifier: Modifier) {
    val onClick = (item as? LibraryItem.Folder)?.let { folder -> { onOpenFolder(folder.file) } }
    val icon = when (item) {
        is LibraryItem.Folder -> Res.drawable.ic_folder
        is LibraryItem.Score -> Res.drawable.ic_music_note
    }
    Tile(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            IconBadge(icon, size = 48.dp)
            Text(item.displayName, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyLibrary(modifier: Modifier) {
    val strings = LocalStrings.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(Res.drawable.ic_library_music, size = 112.dp)
        Text(strings.libraryEmptyTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            strings.libraryEmptyText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
