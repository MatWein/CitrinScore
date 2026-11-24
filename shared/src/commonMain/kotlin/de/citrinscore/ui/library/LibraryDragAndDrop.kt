package de.citrinscore.ui.library

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.library.LibraryItem
import de.citrinscore.library.TransferMode
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_upward
import de.citrinscore.resources.ic_content_copy
import de.citrinscore.resources.ic_folder
import de.citrinscore.resources.ic_music_note
import de.citrinscore.resources.ic_open_with
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToInt

private const val PARENT_KEY = "\u0000parent"
private val PreviewSize = 64.dp

/** Tracks the tiles and drop zones of the library grid and the items that are currently dragged across them. */
internal class LibraryDragState {
    private var container: LayoutCoordinates? = null
    private val bounds = HashMap<String, LayoutCoordinates>()
    private var targets: Map<String, PlatformFile> = emptyMap()

    var dragged by mutableStateOf<List<LibraryItem>>(emptyList())
        private set
    var position by mutableStateOf(Offset.Zero)
        private set
    var target by mutableStateOf<String?>(null)
        private set

    val isDragging get() = dragged.isNotEmpty()

    /** The height of the area above the parent folder zone, in which dragging near the edges scrolls the grid. */
    val scrollAreaHeight get() = boundsOf(PARENT_KEY)?.top ?: container?.size?.height?.toFloat() ?: 0f

    val containerModifier = Modifier.onPlaced { container = it }

    fun isDragged(item: LibraryItem) = dragged.any { it.file.path == item.file.path }

    fun isTarget(item: LibraryItem) = target == item.file.path

    val isParentTarget get() = target == PARENT_KEY

    internal fun register(key: String, coordinates: LayoutCoordinates) {
        bounds[key] = coordinates
    }

    internal fun unregister(key: String, coordinates: LayoutCoordinates?) {
        if (coordinates == null || bounds[key] === coordinates) bounds.remove(key)
    }

    fun itemAt(position: Offset, items: List<LibraryItem>): LibraryItem? =
        items.firstOrNull { item -> boundsOf(item.file.path)?.contains(position) == true }

    fun start(items: List<LibraryItem>, visible: List<LibraryItem>, parent: PlatformFile?, at: Offset) {
        val paths = items.mapTo(HashSet()) { it.file.path }
        targets = visible.filterIsInstance<LibraryItem.Folder>().filter { it.file.path !in paths }.associate { it.file.path to it.file } +
            listOfNotNull(parent?.let { PARENT_KEY to it })
        dragged = items
        move(at)
    }

    fun move(to: Offset) {
        position = to
        refreshTarget()
    }

    fun refreshTarget() {
        target = PARENT_KEY.takeIf { it in targets && boundsOf(it)?.contains(position) == true }
            ?: targets.keys.firstOrNull { it != PARENT_KEY && boundsOf(it)?.contains(position) == true }
    }

    fun targetFile(): PlatformFile? = target?.let(targets::get)

    fun end() {
        dragged = emptyList()
        target = null
        targets = emptyMap()
    }

    private fun boundsOf(key: String): Rect? {
        val parent = container?.takeIf { it.isAttached } ?: return null
        val child = bounds[key]?.takeIf { it.isAttached } ?: return null
        return parent.localBoundingBoxOf(child)
    }
}

/** Makes the bounds of a tile or drop zone known to the drag state as long as it is shown. */
@Composable
internal fun Modifier.dragBounds(state: LibraryDragState, key: String): Modifier {
    var coordinates by androidx.compose.runtime.remember { mutableStateOf<LayoutCoordinates?>(null) }
    DisposableEffect(state, key) { onDispose { state.unregister(key, coordinates) } }
    return onPlaced {
        coordinates = it
        state.register(key, it)
    }
}

private enum class Press { Cancelled, LongPressed, Dragged }

class Drop(val items: List<LibraryItem>, val target: PlatformFile, val position: Offset)

/**
 * Selects a tile on a long press and drags the selection when the pointer moves afterwards. A mouse starts dragging
 * a tile right away. Releasing the pointer over a folder or the parent folder zone reports a [Drop].
 * Without [draggable] it only selects.
 */
@Composable
internal fun Modifier.libraryDragGestures(
    state: LibraryDragState,
    visible: List<LibraryItem>,
    draggable: Boolean,
    parent: PlatformFile?,
    onLongPress: (LibraryItem) -> Unit,
    selectedItems: () -> List<LibraryItem>,
    onDrop: (Drop) -> Unit,
): Modifier {
    val haptics = LocalHapticFeedback.current
    val currentVisible by rememberUpdatedState(visible)
    val currentParent by rememberUpdatedState(parent)
    val currentDraggable by rememberUpdatedState(draggable)
    val currentLongPress by rememberUpdatedState(onLongPress)
    val currentSelection by rememberUpdatedState(selectedItems)
    val currentDrop by rememberUpdatedState(onDrop)
    return pointerInput(state) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            val isMouse = down.type == PointerType.Mouse
            if (isMouse && !currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
            val item = state.itemAt(down.position, currentVisible) ?: return@awaitEachGesture
            val slop = viewConfiguration.touchSlop
            val press = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) { awaitPress(down, slop, isMouse) } ?: Press.LongPressed
            if (press == Press.Cancelled || press == Press.Dragged && !currentDraggable) return@awaitEachGesture

            if (press == Press.LongPressed) haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            currentLongPress(item)
            var position = currentEvent.changes.firstOrNull { it.id == down.id }?.position ?: down.position
            val startDrag = { if (currentDraggable) state.start(currentSelection().ifEmpty { listOf(item) }, currentVisible, currentParent, position) }
            if (press == Press.Dragged) startDrag()
            try {
                while (true) {
                    val event = awaitPointerEvent(PointerEventPass.Initial)
                    val change = event.changes.firstOrNull { it.id == down.id } ?: break
                    event.changes.forEach(PointerInputChange::consume)
                    position = change.position
                    if (!change.pressed) {
                        val target = state.targetFile()
                        if (state.isDragging && target != null) currentDrop(Drop(state.dragged, target, position))
                        break
                    }
                    if (!state.isDragging && (position - down.position).getDistance() > slop) startDrag()
                    if (state.isDragging) state.move(position)
                }
            } finally {
                state.end()
            }
        }
    }
}

/** Waits until the pointer is released, moved beyond the touch slop or joined by another pointer. */
private suspend fun AwaitPointerEventScope.awaitPress(down: PointerInputChange, slop: Float, isMouse: Boolean): Press {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.changes.size > 1) return Press.Cancelled
        val change = event.changes.firstOrNull { it.id == down.id } ?: return Press.Cancelled
        if (!change.pressed || change.isConsumed) return Press.Cancelled
        if ((change.position - down.position).getDistance() > slop) {
            if (!isMouse) return Press.Cancelled
            change.consume()
            return Press.Dragged
        }
    }
}

/** A small stack of the dragged items that follows the pointer. */
@Composable
internal fun DragPreview(state: LibraryDragState) {
    val first = state.dragged.firstOrNull() ?: return
    val half = with(androidx.compose.ui.platform.LocalDensity.current) { (PreviewSize / 2).toPx() }
    Box(Modifier.offset { IntOffset((state.position.x - half).roundToInt(), (state.position.y - half * 2).roundToInt()) }) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 6.dp,
            modifier = Modifier.size(PreviewSize),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painterResource(if (first is LibraryItem.Folder) Res.drawable.ic_folder else Res.drawable.ic_music_note),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        if (state.dragged.size > 1) {
            Badge(Modifier.align(Alignment.TopEnd).offset(x = Spacing.Small, y = -Spacing.Small)) { Text(state.dragged.size.toString()) }
        }
    }
}

/** The zone at the bottom edge into which dragged items are dropped to put them into the parent folder. */
@Composable
internal fun ParentDropZone(state: LibraryDragState, name: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val active = state.isParentTarget
    Surface(
        shape = MaterialTheme.shapes.large,
        color = if (active) colors.primaryContainer else colors.surfaceContainerHighest,
        contentColor = if (active) colors.onPrimaryContainer else colors.onSurface,
        shadowElevation = 3.dp,
        border = if (active) androidx.compose.foundation.BorderStroke(2.dp, colors.primary) else null,
        modifier = modifier.fillMaxWidth().padding(Spacing.Large).dragBounds(state, PARENT_KEY),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.ExtraLarge, vertical = Spacing.Large),
            horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(painterResource(Res.drawable.ic_arrow_upward), contentDescription = null)
            Text(LocalStrings.current.parentFolder(name), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** Asks whether the dropped items are moved or copied, at the position where they were dropped. */
@Composable
internal fun DropMenu(drop: Drop, onTransfer: (TransferMode) -> Unit, onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    Box(Modifier.offset { IntOffset(drop.position.x.roundToInt(), drop.position.y.roundToInt()) }) {
        DropdownMenu(expanded = true, onDismissRequest = onDismiss) {
            DropdownMenuItem(
                text = { Text(strings.moveHere) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_open_with), contentDescription = null) },
                onClick = { onTransfer(TransferMode.Move) },
            )
            DropdownMenuItem(
                text = { Text(strings.copyHere) },
                leadingIcon = { Icon(painterResource(Res.drawable.ic_content_copy), contentDescription = null) },
                onClick = { onTransfer(TransferMode.Copy) },
            )
        }
    }
}
