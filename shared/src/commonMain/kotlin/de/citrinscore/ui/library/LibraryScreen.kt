package de.citrinscore.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.library.FolderContent
import de.citrinscore.library.LibraryItem
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.resources.ic_create_new_folder
import de.citrinscore.resources.ic_delete
import de.citrinscore.resources.ic_folder
import de.citrinscore.resources.ic_library_music
import de.citrinscore.resources.ic_more_vert
import de.citrinscore.resources.ic_music_note
import de.citrinscore.resources.ic_refresh
import de.citrinscore.resources.ic_settings
import de.citrinscore.resources.ic_upload_file
import de.citrinscore.score.CitScore
import de.citrinscore.ui.components.ConfirmDeleteDialog
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.IconBadge
import de.citrinscore.ui.components.IconBadgeSize
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.components.VerticalScrollIndicator
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.painterResource

private val FabClearance = 96.dp
private val TileMinWidth = 160.dp
private val TilePadding = PaddingValues(Spacing.Medium)
private val TileMenuSize = Spacing.Section

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    onImport: () -> Unit,
    onOpenScore: (PlatformFile) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<LibraryItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                when (error) {
                    LibraryError.Load -> strings.folderLoadFailed
                    LibraryError.CreateFolder -> strings.createFolderFailed
                    LibraryError.Delete -> strings.deleteFailed
                },
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (state.isRoot) strings.libraryTitle else state.currentFolder.name,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    if (!state.isRoot) {
                        IconAction(Res.drawable.ic_arrow_back, strings.back, onClick = viewModel::up)
                    }
                },
                actions = {
                    IconAction(Res.drawable.ic_refresh, strings.refresh, onClick = viewModel::refresh)
                    IconAction(
                        Res.drawable.ic_create_new_folder,
                        strings.newFolder,
                        onClick = { showNewFolderDialog = true },
                        enabled = state.content != null,
                    )
                    IconAction(Res.drawable.ic_settings, strings.settings, onClick = onOpenSettings)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onImport,
                icon = { Icon(painterResource(Res.drawable.ic_upload_file), contentDescription = null) },
                text = { Text(strings.importScores) },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            state.content?.let { content ->
                if (content.isEmpty) {
                    EmptyLibrary(
                        title = if (state.isRoot) strings.libraryEmptyTitle else strings.folderEmptyTitle,
                        modifier = Modifier.align(Alignment.Center),
                    )
                } else {
                    LibraryGrid(
                        content = content,
                        onOpenFolder = viewModel::open,
                        onOpenScore = onOpenScore,
                        onDelete = { pendingDelete = it },
                        loadThumbnail = viewModel::thumbnail,
                    )
                }
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

    pendingDelete?.let { item ->
        ConfirmDeleteDialog(
            text = when (item) {
                is LibraryItem.Folder -> strings.deleteFolderText(item.displayName)
                is LibraryItem.Score -> strings.deleteScoreText(item.displayName)
            },
            onConfirm = {
                viewModel.delete(item.file)
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun LibraryGrid(
    content: FolderContent,
    onOpenFolder: (PlatformFile) -> Unit,
    onOpenScore: (PlatformFile) -> Unit,
    onDelete: (LibraryItem) -> Unit,
    loadThumbnail: suspend (PlatformFile) -> ImageBitmap?,
) {
    val gridState = rememberLazyGridState()
    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(TileMinWidth),
            state = gridState,
            contentPadding = PaddingValues(start = Spacing.Large, top = Spacing.Large, end = Spacing.Large, bottom = FabClearance),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(content.items, key = { it.file.path }) { item ->
                val onClick = when (item) {
                    is LibraryItem.Folder -> onOpenFolder
                    is LibraryItem.Score -> onOpenScore
                }
                LibraryTile(item, onClick = { onClick(item.file) }, onDelete = { onDelete(item) }, loadThumbnail)
            }
        }
        VerticalScrollIndicator(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = Spacing.ExtraSmall))
    }
}

@Composable
private fun LibraryTile(
    item: LibraryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    loadThumbnail: suspend (PlatformFile) -> ImageBitmap?,
) {
    Tile(Modifier.aspectRatio(CitScore.PAGE_ASPECT_RATIO), onClick = onClick, contentPadding = TilePadding) {
        Row {
            Text(
                item.displayName,
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(top = Spacing.ExtraSmall),
            )
            TileMenu(onDelete)
        }
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center,
        ) {
            when (item) {
                is LibraryItem.Folder -> IconBadge(Res.drawable.ic_folder, size = IconBadgeSize.Large)
                is LibraryItem.Score -> ScorePreview(item.file, loadThumbnail)
            }
        }
    }
}

@Composable
private fun TileMenu(onDelete: () -> Unit) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconAction(Res.drawable.ic_more_vert, strings.moreOptions, onClick = { expanded = true }, modifier = Modifier.size(TileMenuSize))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(
                text = { Text(strings.delete) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_delete), contentDescription = null) },
                onClick = {
                    expanded = false
                    onDelete()
                },
            )
        }
    }
}

@Composable
private fun ScorePreview(score: PlatformFile, loadThumbnail: suspend (PlatformFile) -> ImageBitmap?) {
    val thumbnail by produceState<ImageBitmap?>(null, score) { value = loadThumbnail(score) }
    val image = thumbnail
    if (image == null) {
        Icon(painterResource(Res.drawable.ic_music_note), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    } else {
        Image(image, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alignment = Alignment.TopCenter)
    }
}

@Composable
private fun EmptyLibrary(title: String, modifier: Modifier) {
    val strings = LocalStrings.current
    Column(modifier.padding(Spacing.ExtraLarge), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        IconBadge(Res.drawable.ic_library_music, size = IconBadgeSize.Hero)
        Text(title, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            strings.libraryEmptyText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
