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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.library.LibraryItem
import de.citrinscore.library.LibrarySort
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.resources.ic_check
import de.citrinscore.resources.ic_close
import de.citrinscore.resources.ic_create_new_folder
import de.citrinscore.resources.ic_delete
import de.citrinscore.resources.ic_edit
import de.citrinscore.resources.ic_folder
import de.citrinscore.resources.ic_library_music
import de.citrinscore.resources.ic_more_vert
import de.citrinscore.resources.ic_music_note
import de.citrinscore.resources.ic_refresh
import de.citrinscore.resources.ic_search
import de.citrinscore.resources.ic_settings
import de.citrinscore.resources.ic_sort
import de.citrinscore.resources.ic_sync
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
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val FabClearance = 96.dp
private val TileMinWidth = 160.dp
private val TilePadding = PaddingValues(Spacing.Medium)
private val TileMenuSize = Spacing.Section
private val WideToolbarWidth = 600.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    sort: LibrarySort,
    onSortChange: (LibrarySort) -> Unit,
    onImport: () -> Unit,
    onOpenScore: (PlatformFile) -> Unit,
    onEditScore: (PlatformFile) -> Unit,
    onOpenSettings: () -> Unit,
    onSync: (() -> Unit)? = null,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<LibraryItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    var query by rememberSaveable(state.currentFolder.path) { mutableStateOf<String?>(null) }
    var searchOpened by remember { mutableStateOf(false) }

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = query != null || !state.isRoot,
        onBackCompleted = { if (query != null) query = null else viewModel.up() },
    )

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                when (error) {
                    LibraryError.Load -> strings.folderLoadFailed
                    LibraryError.CreateFolder -> strings.createFolderFailed
                    LibraryError.Delete -> strings.deleteFailed
                    LibraryError.SaveSettings -> strings.settingsSaveFailed
                },
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val search = query
                    if (search == null) {
                        Text(
                            if (state.isRoot) strings.libraryTitle else state.currentFolder.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        SearchField(search, requestFocus = searchOpened, onChange = { query = it })
                    }
                },
                navigationIcon = {
                    if (!state.isRoot) {
                        IconAction(Res.drawable.ic_arrow_back, strings.back, onClick = viewModel::up)
                    }
                },
                actions = {
                    if (query == null) {
                        IconAction(Res.drawable.ic_search, strings.search, onClick = {
                            searchOpened = true
                            query = ""
                        })
                    } else {
                        IconAction(Res.drawable.ic_close, strings.closeSearch, onClick = { query = null })
                    }
                    SortMenu(sort, onSortChange)
                    onSync?.let { IconAction(Res.drawable.ic_sync, strings.sync, onClick = it) }
                    OverflowActions(
                        listOf(
                            ToolbarAction(Res.drawable.ic_refresh, strings.refresh, onClick = viewModel::refresh),
                            ToolbarAction(Res.drawable.ic_create_new_folder, strings.newFolder, enabled = state.content != null) {
                                showNewFolderDialog = true
                            },
                            ToolbarAction(Res.drawable.ic_settings, strings.settings, onClick = onOpenSettings),
                        ),
                    )
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
                        items = content.arranged(sort, query.orEmpty()),
                        onOpenFolder = viewModel::open,
                        onOpenScore = onOpenScore,
                        onEdit = onEditScore,
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
    items: List<LibraryItem>,
    onOpenFolder: (PlatformFile) -> Unit,
    onOpenScore: (PlatformFile) -> Unit,
    onEdit: (PlatformFile) -> Unit,
    onDelete: (LibraryItem) -> Unit,
    loadThumbnail: suspend (PlatformFile) -> ImageBitmap?,
) {
    val gridState = rememberLazyGridState()
    Box(Modifier.fillMaxSize()) {
        if (items.isEmpty()) {
            Text(
                LocalStrings.current.noSearchResults,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(TileMinWidth),
            state = gridState,
            contentPadding = PaddingValues(start = Spacing.Large, top = Spacing.Large, end = Spacing.Large, bottom = FabClearance),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.file.path }) { item ->
                val onClick = when (item) {
                    is LibraryItem.Folder -> onOpenFolder
                    is LibraryItem.Score -> onOpenScore
                }
                LibraryTile(
                    item = item,
                    onClick = { onClick(item.file) },
                    onEdit = { onEdit(item.file) }.takeIf { item is LibraryItem.Score },
                    onDelete = { onDelete(item) },
                    loadThumbnail = loadThumbnail,
                )
            }
        }
        VerticalScrollIndicator(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = Spacing.ExtraSmall))
    }
}

@Composable
private fun LibraryTile(
    item: LibraryItem,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
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
            TileMenu(onEdit, onDelete)
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
private fun SearchField(query: String, requestFocus: Boolean, onChange: (String) -> Unit) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (requestFocus) focusRequester.requestFocus() }
    TextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text(LocalStrings.current.searchPlaceholder) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
    )
}

private class ToolbarAction(val icon: DrawableResource, val label: String, val enabled: Boolean = true, val onClick: () -> Unit)

@Composable
private fun OverflowActions(actions: List<ToolbarAction>) {
    val width = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    if (width >= WideToolbarWidth) {
        actions.forEach { IconAction(it.icon, it.label, onClick = it.onClick, enabled = it.enabled) }
        return
    }
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconAction(Res.drawable.ic_more_vert, LocalStrings.current.moreActions, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            actions.forEach { action ->
                DropdownMenuItem(
                    text = { Text(action.label) },
                    leadingIcon = { Icon(painterResource(action.icon), contentDescription = null) },
                    enabled = action.enabled,
                    onClick = {
                        expanded = false
                        action.onClick()
                    },
                )
            }
        }
    }
}

@Composable
private fun SortMenu(selected: LibrarySort, onSelect: (LibrarySort) -> Unit) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconAction(Res.drawable.ic_sort, strings.sort, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LibrarySort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label(strings)) },
                    onClick = {
                        expanded = false
                        onSelect(sort)
                    },
                    trailingIcon = {
                        if (sort == selected) Icon(painterResource(Res.drawable.ic_check), contentDescription = null)
                    },
                )
            }
        }
    }
}

private fun LibrarySort.label(strings: Strings) = when (this) {
    LibrarySort.RecentlyUsed -> strings.sortRecentlyUsed
    LibrarySort.Title -> strings.sortTitle
}

@Composable
private fun TileMenu(onEdit: (() -> Unit)?, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    val item: @Composable (String, DrawableResource, () -> Unit) -> Unit = { label, icon, action ->
        DropdownMenuItem(
            text = { Text(label) },
            leadingIcon = { Icon(painterResource(icon), contentDescription = null) },
            onClick = {
                expanded = false
                action()
            },
        )
    }
    Box {
        IconAction(Res.drawable.ic_more_vert, strings.moreOptions, onClick = { expanded = true }, modifier = Modifier.size(TileMenuSize))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            onEdit?.let { item(strings.edit, Res.drawable.ic_edit, it) }
            item(strings.delete, Res.drawable.ic_delete, onDelete)
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
