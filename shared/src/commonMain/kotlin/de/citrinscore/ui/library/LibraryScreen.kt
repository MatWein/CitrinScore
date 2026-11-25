package de.citrinscore.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.scrollBy
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Badge
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isShiftPressed
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
import de.citrinscore.library.Highlighted
import de.citrinscore.library.LibraryItem
import de.citrinscore.library.LibrarySort
import de.citrinscore.library.SearchEntry
import de.citrinscore.library.SearchHit
import de.citrinscore.library.canShowInFileManager
import de.citrinscore.library.search
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.resources.ic_check
import de.citrinscore.resources.ic_check_circle
import de.citrinscore.resources.ic_close
import de.citrinscore.resources.ic_create_new_folder
import de.citrinscore.resources.ic_delete
import de.citrinscore.resources.ic_download
import de.citrinscore.resources.ic_edit
import de.citrinscore.resources.ic_folder
import de.citrinscore.resources.ic_library_music
import de.citrinscore.resources.ic_more_vert
import de.citrinscore.resources.ic_music_note
import de.citrinscore.resources.ic_refresh
import de.citrinscore.resources.ic_search
import de.citrinscore.resources.ic_select_all
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
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val FabClearance = 96.dp
private val TileMinWidth = 160.dp
private val TilePadding = PaddingValues(Spacing.Medium)
private val TileMenuSize = Spacing.Section
private val WideToolbarWidth = 600.dp
private const val ZIP_EXTENSION = "zip"
private const val DRAGGED_ALPHA = 0.4f
private const val SEARCH_DELAY = 150L
private const val LOCATION_SEPARATOR = " › "
private val AutoScrollEdge = 64.dp
private val AutoScrollSpeed = 16.dp
private val SelectionMarkBorder = 2.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel,
    sort: LibrarySort,
    onSortChange: (LibrarySort) -> Unit,
    onImport: () -> Unit,
    onOpenScore: (score: PlatformFile, folder: PlatformFile) -> Unit,
    onEditScore: (score: PlatformFile, folder: PlatformFile) -> Unit,
    onOpenSettings: () -> Unit,
    onSync: (() -> Unit)? = null,
    localChanges: Int = 0,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current
    var showNewFolderDialog by rememberSaveable { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<List<LibraryItem>?>(null) }
    var pendingDrop by remember { mutableStateOf<Drop?>(null) }
    var anchor by remember { mutableStateOf<String?>(null) }
    val drag = remember { LibraryDragState() }
    var exporting by remember { mutableStateOf<PlatformFile?>(null) }
    val exporter = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { target ->
        val score = exporting
        exporting = null
        if (target != null && score != null) viewModel.export(score, target)
    }
    val snackbarHostState = remember { SnackbarHostState() }
    var query by rememberSaveable(state.currentFolder.path) { mutableStateOf<String?>(null) }
    var searchOpened by remember { mutableStateOf(false) }
    LaunchedEffect(query != null) { viewModel.setSearching(query != null) }
    val search by produceState(query.orEmpty().trim(), query) {
        val text = query.orEmpty().trim()
        if (text.isNotEmpty()) delay(SEARCH_DELAY)
        value = text
    }
    val hits = remember(state.content, state.searchEntries, sort, search) {
        val folder = state.currentFolder
        val content = state.content
        when {
            content == null -> emptyList()
            search.isEmpty() -> content.arranged(sort).map { SearchHit(SearchEntry(it, folder), Highlighted(it.displayName)) }
            else -> (state.searchEntries ?: content.items.map { SearchEntry(it, folder) }).search(search, sort)
        }
    }
    val visible = remember(hits) { hits.map { it.item } }
    LaunchedEffect(visible) { viewModel.retainSelection(visible.mapTo(HashSet()) { it.file.path }) }
    val windowInfo = LocalWindowInfo.current

    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = state.isSelecting || query != null || !state.isRoot,
        onBackCompleted = {
            when {
                viewModel.state.value.isSelecting -> viewModel.clearSelection()
                query != null -> query = null
                else -> viewModel.up()
            }
        },
    )

    fun toggle(item: LibraryItem) {
        val path = item.file.path
        val selection = viewModel.state.value.selection
        viewModel.setSelection(if (path in selection) selection - path else selection + path)
        anchor = path
    }

    @OptIn(ExperimentalComposeUiApi::class)
    fun onTileClick(hit: SearchHit) {
        val item = hit.item
        val keys = windowInfo.keyboardModifiers
        val selection = viewModel.state.value.selection
        val from = visible.indexOfFirst { it.file.path == anchor }
        val to = visible.indexOfFirst { it.file.path == item.file.path }
        when {
            keys.isShiftPressed && from >= 0 && to >= 0 ->
                viewModel.setSelection(selection + visible.subList(minOf(from, to), maxOf(from, to) + 1).map { it.file.path })
            keys.isShiftPressed || keys.isCtrlPressed || keys.isMetaPressed || selection.isNotEmpty() -> toggle(item)
            item is LibraryItem.Folder -> viewModel.open(*(hit.entry.path + item.file).toTypedArray())
            item is LibraryItem.Score -> onOpenScore(item.file, hit.entry.folder)
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { error ->
            snackbarHostState.showSnackbar(
                when (error) {
                    LibraryError.Load -> strings.folderLoadFailed
                    LibraryError.CreateFolder -> strings.createFolderFailed
                    LibraryError.Delete -> strings.deleteFailed
                    LibraryError.Export -> strings.exportFailed
                    LibraryError.ShowInFileManager -> strings.showFilesExternallyFailed
                    LibraryError.Move -> strings.moveFailed
                    LibraryError.Copy -> strings.copyFailed
                    LibraryError.SaveSettings -> strings.settingsSaveFailed
                },
            )
            viewModel.dismissError()
        }
    }

    Scaffold(
        topBar = {
            if (state.isSelecting) {
                SelectionBar(
                    count = state.selection.size,
                    onClear = viewModel::clearSelection,
                    onSelectAll = { viewModel.setSelection(visible.mapTo(HashSet()) { it.file.path }) },
                    onDelete = { pendingDelete = visible.filter(state::isSelected) },
                )
            } else TopAppBar(
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
                    onSync?.let { SyncAction(localChanges, it) }
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
            if (!state.isSelecting) {
                ExtendedFloatingActionButton(
                    onClick = onImport,
                    icon = { Icon(painterResource(Res.drawable.ic_upload_file), contentDescription = null) },
                    text = { Text(strings.importScores) },
                )
            }
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
                        hits = hits,
                        selection = state.selection,
                        drag = drag,
                        onClick = ::onTileClick,
                        onEdit = onEditScore,
                        onExport = { score ->
                            exporting = score
                            exporter.launch(suggestedName = score.name, defaultExtension = ZIP_EXTENSION)
                        },
                        onDelete = { pendingDelete = listOf(it) },
                        onShowExternally = viewModel::showInFileManager,
                        loadThumbnail = viewModel::thumbnail,
                        modifier = drag.containerModifier.libraryDragGestures(
                            state = drag,
                            visible = visible,
                            draggable = search.isEmpty(),
                            parent = state.parentFolder,
                            onLongPress = { item ->
                                viewModel.setSelection(viewModel.state.value.selection + item.file.path)
                                anchor = item.file.path
                            },
                            selectedItems = { visible.filter { it.file.path in viewModel.state.value.selection } },
                            onDrop = { pendingDrop = it },
                        ),
                    )
                    val parent = state.parentFolder
                    if (drag.isDragging && parent != null) {
                        ParentDropZone(
                            state = drag,
                            name = if (state.path.size == 2) strings.libraryTitle else parent.name,
                            modifier = Modifier.align(Alignment.BottomCenter),
                        )
                    }
                    DragPreview(drag)
                    pendingDrop?.let { drop ->
                        DropMenu(
                            drop = drop,
                            onTransfer = { mode ->
                                pendingDrop = null
                                viewModel.transfer(drop.items.map { it.file }, drop.target, mode)
                            },
                            onDismiss = { pendingDrop = null },
                        )
                    }
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

    pendingDelete?.let { items ->
        val single = items.singleOrNull()
        ConfirmDeleteDialog(
            text = when (single) {
                is LibraryItem.Folder -> strings.deleteFolderText(single.displayName)
                is LibraryItem.Score -> strings.deleteScoreText(single.displayName)
                null -> strings.deleteItemsText(items.size)
            },
            onConfirm = {
                viewModel.delete(items.map { it.file })
                pendingDelete = null
            },
            onDismiss = { pendingDelete = null },
        )
    }
}

@Composable
private fun LibraryGrid(
    hits: List<SearchHit>,
    selection: Set<String>,
    drag: LibraryDragState,
    onClick: (SearchHit) -> Unit,
    onEdit: (score: PlatformFile, folder: PlatformFile) -> Unit,
    onExport: (PlatformFile) -> Unit,
    onDelete: (LibraryItem) -> Unit,
    onShowExternally: (PlatformFile) -> Unit,
    loadThumbnail: suspend (PlatformFile) -> ImageBitmap?,
    modifier: Modifier,
) {
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current
    LaunchedEffect(drag.isDragging) {
        if (!drag.isDragging) return@LaunchedEffect
        val edge = with(density) { AutoScrollEdge.toPx() }
        val speed = with(density) { AutoScrollSpeed.toPx() }
        while (true) {
            withFrameNanos {}
            val y = drag.position.y
            val bottom = drag.scrollAreaHeight
            val delta = when {
                y < edge -> -speed * (1 - y / edge).coerceIn(0f, 1f)
                y in (bottom - edge)..bottom -> speed * (1 - (bottom - y) / edge).coerceIn(0f, 1f)
                else -> 0f
            }
            if (delta != 0f) gridState.scrollBy(delta)
            drag.refreshTarget()
        }
    }
    val selecting = selection.isNotEmpty()
    Box(modifier.fillMaxSize()) {
        if (hits.isEmpty()) {
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
            items(hits, key = { it.item.file.path }) { hit ->
                val item = hit.item
                LibraryTile(
                    hit = hit,
                    selected = item.file.path in selection,
                    selecting = selecting,
                    highlighted = drag.isTarget(item),
                    dimmed = drag.isDragged(item),
                    modifier = Modifier.dragBounds(drag, item.file.path),
                    onClick = { onClick(hit) },
                    onEdit = { onEdit(item.file, hit.entry.folder) }.takeIf { item is LibraryItem.Score },
                    onExport = { onExport(item.file) }.takeIf { item is LibraryItem.Score },
                    onDelete = { onDelete(item) },
                    onShowExternally = { onShowExternally(item.file) }.takeIf { item.file.canShowInFileManager },
                    loadThumbnail = loadThumbnail,
                )
            }
        }
        VerticalScrollIndicator(gridState, Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = Spacing.ExtraSmall))
    }
}

@Composable
private fun LibraryTile(
    hit: SearchHit,
    selected: Boolean,
    selecting: Boolean,
    highlighted: Boolean,
    dimmed: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    onExport: (() -> Unit)?,
    onDelete: () -> Unit,
    onShowExternally: (() -> Unit)?,
    loadThumbnail: suspend (PlatformFile) -> ImageBitmap?,
) {
    val item = hit.item
    Tile(
        modifier.aspectRatio(CitScore.PAGE_ASPECT_RATIO).alpha(if (dimmed) DRAGGED_ALPHA else 1f),
        selected = selected || highlighted,
        onClick = onClick,
        contentPadding = TilePadding,
    ) {
        Row {
            Text(
                hit.title.annotated(),
                style = MaterialTheme.typography.titleSmall,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(top = Spacing.ExtraSmall),
            )
            if (selecting) SelectionMark(selected) else TileMenu(onEdit, onExport, onShowExternally, onDelete)
        }
        hit.detail?.let { detail ->
            Text(
                detail.annotated(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (hit.entry.path.isNotEmpty()) {
            Text(
                hit.entry.location.joinToString(LOCATION_SEPARATOR),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(bottom = Spacing.ExtraSmall),
            )
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
private fun Highlighted.annotated(): AnnotatedString {
    val style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    return buildAnnotatedString {
        append(text)
        matches.forEach { addStyle(style, it.first, it.last + 1) }
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
private fun TileMenu(onEdit: (() -> Unit)?, onExport: (() -> Unit)?, onShowExternally: (() -> Unit)?, onDelete: () -> Unit) {
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
            onExport?.let { item(strings.export, Res.drawable.ic_download, it) }
            onShowExternally?.let { item(strings.showFilesExternally, Res.drawable.ic_folder, it) }
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

@Composable
private fun SyncAction(localChanges: Int, onClick: () -> Unit) {
    val strings = LocalStrings.current
    IconAction(
        icon = Res.drawable.ic_sync,
        label = if (localChanges > 0) strings.syncWithChanges(localChanges) else strings.sync,
        onClick = onClick,
        badge = { if (localChanges > 0) Badge { Text(localChanges.toString()) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectionBar(count: Int, onClear: () -> Unit, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    val strings = LocalStrings.current
    TopAppBar(
        title = { Text(strings.selectedCount(count)) },
        navigationIcon = { IconAction(Res.drawable.ic_close, strings.clearSelection, onClick = onClear) },
        actions = {
            IconAction(Res.drawable.ic_select_all, strings.selectAll, onClick = onSelectAll)
            IconAction(Res.drawable.ic_delete, strings.delete, onClick = onDelete)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    )
}

@Composable
private fun SelectionMark(selected: Boolean) {
    Box(Modifier.size(TileMenuSize), contentAlignment = Alignment.Center) {
        if (selected) {
            Icon(painterResource(Res.drawable.ic_check_circle), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        } else {
            Box(Modifier.size(Spacing.ExtraLarge - Spacing.ExtraSmall).border(SelectionMarkBorder, MaterialTheme.colorScheme.outline, CircleShape))
        }
    }
}
