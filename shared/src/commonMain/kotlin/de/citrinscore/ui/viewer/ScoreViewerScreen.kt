package de.citrinscore.ui.viewer

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.keepScreenOn
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.resources.ic_arrow_downward
import de.citrinscore.resources.ic_check
import de.citrinscore.resources.ic_contrast
import de.citrinscore.resources.ic_image
import de.citrinscore.resources.ic_library_music
import de.citrinscore.resources.ic_piano
import de.citrinscore.resources.ic_filter_1
import de.citrinscore.resources.ic_edit
import de.citrinscore.resources.ic_height
import de.citrinscore.resources.ic_open_with
import de.citrinscore.resources.ic_width
import de.citrinscore.resources.ic_music_note
import de.citrinscore.score.PartDisplay
import de.citrinscore.score.ScrollMode
import de.citrinscore.score.ShownInstruments
import de.citrinscore.score.musicxml.NotePositions
import de.citrinscore.ui.WindowTitle
import de.citrinscore.ui.viewer.playback.PlaybackCursor
import de.citrinscore.ui.viewer.playback.PlaybackBar
import de.citrinscore.ui.viewer.playback.PlaybackViewModel
import de.citrinscore.resources.ic_zoom_in
import de.citrinscore.library.PartRef
import de.citrinscore.ui.parts.partName
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.LabelTooltip
import de.citrinscore.ui.theme.LocalDarkTheme
import de.citrinscore.ui.theme.Spacing
import de.citrinscore.ui.viewer.annotations.AnnotationDialogs
import de.citrinscore.ui.viewer.annotations.AnnotationInput
import de.citrinscore.ui.viewer.annotations.AnnotationToolbar
import de.citrinscore.ui.viewer.annotations.AnnotationViewModel
import de.citrinscore.ui.viewer.annotations.staffSizing
import de.citrinscore.ui.viewer.audio.AudioPlayerBar
import de.citrinscore.ui.viewer.audio.RecordingBar
import de.citrinscore.ui.viewer.audio.OnAppBackground
import de.citrinscore.ui.viewer.audio.ScoreAudioViewModel
import de.citrinscore.ui.viewer.files.AttachmentsMenu
import de.citrinscore.ui.viewer.files.ScoreFilesViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToInt

private const val SAVE_DELAY_MILLIS = 1000L
private const val REFLOW_DELAY_MILLIS = 400L
private const val PERCENT = 100
private const val CURSOR_ALPHA = 0.25f
private const val SUBTITLE_SEPARATOR = " · "
private val ZoomSliderWidth = 320.dp
private val ZoomLabelWidth = 56.dp
private val ToggleSegmentSize = DpSize(40.dp, 28.dp)
private val ToggleIconSize = 18.dp
private const val TOGGLE_ALPHA = 0.85f
private val StepButtonSize = 56.dp
private val StepIconSize = 32.dp
private const val IDLE_MILLIS = 10_000L
private const val IDLE_OVERLAY_ALPHA = 0.25f
private const val OVERLAY_FADE_MILLIS = 600

@Composable
fun ScoreViewerScreen(
    viewModel: ScoreViewModel,
    audioViewModel: ScoreAudioViewModel,
    annotationViewModel: AnnotationViewModel,
    filesViewModel: ScoreFilesViewModel,
    playbackViewModel: PlaybackViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scrollMode by viewModel.scrollMode.collectAsStateWithLifecycle()
    val ready = state as? ScoreViewerState.Ready
    val viewport = ready?.let { remember(it) { ScoreViewport(it.view) } }
    val toolbar = ToolbarState(ready, viewport, viewModel, audioViewModel, annotationViewModel, filesViewModel, playbackViewModel, onClose)
    WindowTitle(toolbar.windowTitle())
    val playback by playbackViewModel.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(viewport) { if (viewport != null) focusRequester.requestFocus() }
    SideEffect { viewport?.scrollMode = scrollMode }
    val scope = rememberCoroutineScope()
    var activity by remember { mutableIntStateOf(0) }
    var idle by remember { mutableStateOf(false) }
    LaunchedEffect(activity) {
        idle = false
        delay(IDLE_MILLIS)
        idle = true
    }
    val overlayAlpha by animateFloatAsState(if (idle) IDLE_OVERLAY_ALPHA else 1f, tween(OVERLAY_FADE_MILLIS))
    val overlays = Overlays(overlayAlpha, scrollMode, viewModel::selectScrollMode)

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(paperColors()?.paper ?: Color.White)
            .keepScreenOn()
            .onUserActivity { activity++ }
            .keyboardNavigation(viewport, ready?.reflowScale ?: 1f, scope, focusRequester, if (playback.isOpen) playbackViewModel::step else null),
    ) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize()) {
                SideToolbar(toolbar)
                val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)
                ViewerContent(state, viewport, toolbar, overlays, viewModel, Modifier.weight(1f).fillMaxHeight().windowInsetsPadding(insets))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopToolbar(toolbar)
                val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                ViewerContent(state, viewport, toolbar, overlays, viewModel, Modifier.weight(1f).fillMaxWidth().windowInsetsPadding(insets))
            }
        }
    }
}

private class ToolbarState(
    val ready: ScoreViewerState.Ready?,
    val viewport: ScoreViewport?,
    val score: ScoreViewModel,
    val audio: ScoreAudioViewModel,
    val annotations: AnnotationViewModel,
    val files: ScoreFilesViewModel,
    val playback: PlaybackViewModel,
    val onClose: () -> Unit,
)

private class Overlays(val alpha: Float, val scrollMode: ScrollMode, val onScrollModeChange: (ScrollMode) -> Unit)

@Composable
private fun ViewerContent(
    state: ScoreViewerState,
    viewport: ScoreViewport?,
    toolbar: ToolbarState,
    overlays: Overlays,
    viewModel: ScoreViewModel,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    val audioViewModel = toolbar.audio
    val annotationViewModel = toolbar.annotations
    val audio by audioViewModel.state.collectAsStateWithLifecycle()
    val annotations by annotationViewModel.state.collectAsStateWithLifecycle()
    val playbackViewModel = toolbar.playback
    val playback by playbackViewModel.state.collectAsStateWithLifecycle()
    val density = LocalDensity.current
    Box(
        modifier.onSizeChanged { size -> with(density) { viewModel.updateViewportSize(size.width.toDp().value, size.height.toDp().value) } },
        contentAlignment = Alignment.Center,
    ) {
        when (state) {
            ScoreViewerState.Loading -> CircularProgressIndicator()
            ScoreViewerState.Failed -> Text(strings.scoreLoadFailed, color = MaterialTheme.colorScheme.error)
            is ScoreViewerState.Ready -> if (viewport != null) {
                val render: PageRenderer = remember(state.part, state.display) {
                    { page, region, size -> viewModel.render(state.part, state.display, page, region, size) }
                }
                val anchors by viewModel.anchors.collectAsStateWithLifecycle()
                val editing = annotations.isEditing && (!state.isReflowable || anchors != null)
                val notes by viewModel.notes.collectAsStateWithLifecycle()
                val cursor by playbackViewModel.cursor.collectAsStateWithLifecycle()
                key(state.part, state.display) {
                    ScoreDocument(
                        pageSizes = state.pageSizes,
                        viewport = viewport,
                        render = render,
                        paperColors = paperColors(),
                        annotations = annotations.annotations,
                        annotationInput = AnnotationInput(
                            tool = annotations.tool,
                            onStrokes = annotationViewModel::addStrokes,
                            onErase = annotationViewModel::erase,
                            onTextTap = annotationViewModel::startText,
                            sizing = anchors?.staffSizing(),
                        ).takeIf { editing },
                        highlight = playbackHighlight(cursor, notes),
                        onTap = notes?.takeIf { playback.isOpen && !editing }?.let { positions ->
                            { page, position -> positions.nearest(page, position)?.let(playbackViewModel::select) }
                        },
                        bottomInset = ViewerBarHeight,
                    )
                }
                PersistView(viewport, state.part, state.display, viewModel)
                if (state.isReflowable) ReflowOnZoom(viewport, state.part, viewModel)
                val overlay = Modifier.graphicsLayer { alpha = overlays.alpha }
                if (editing) {
                    AnnotationToolbar(annotations, annotationViewModel, Modifier.align(Alignment.TopCenter).padding(Spacing.Large))
                } else {
                    ScrollModeToggle(overlays.scrollMode, overlays.onScrollModeChange, overlay.align(Alignment.TopStart).padding(Spacing.Small))
                    if (state.displays.size > 1) DisplayToggle(state, viewport, viewModel, overlay.align(Alignment.TopEnd).padding(Spacing.Small))
                }
                val barShown = audio.recordingStart != null || audio.player != null || playback.isOpen
                StepButton(
                    viewport,
                    overlay.align(Alignment.BottomStart).padding(start = Spacing.Large, bottom = Spacing.Large + if (barShown) ViewerBarHeight else 0.dp),
                )
            }
        }
        val barModifier = Modifier.align(Alignment.BottomCenter)
        val player = audio.player
        val recordingStart = audio.recordingStart
        when {
            recordingStart != null -> RecordingBar(recordingStart, audioViewModel::stopRecording, barModifier)
            player != null -> AudioPlayerBar(player, audioViewModel, barModifier)
            playback.isOpen -> PlaybackBar(playback, playbackViewModel, barModifier)
        }
    }
    AnnotationDialogs(annotations, annotationViewModel)
    OnAppBackground {
        playbackViewModel.stop()
        audioViewModel.pause()
        audioViewModel.saveActiveRecording(strings.recordingName(audio.nextRecordingNumber))
    }
}

private fun Modifier.onUserActivity(onActivity: () -> Unit) = this
    .onPreviewKeyEvent { event ->
        if (event.type == KeyEventType.KeyDown) onActivity()
        false
    }
    .pointerInput(onActivity) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val scrolled = event.type == PointerEventType.Scroll
                if (scrolled || event.type == PointerEventType.Press || event.changes.any { it.pressed }) onActivity()
            }
        }
    }

private fun Modifier.keyboardNavigation(
    viewport: ScoreViewport?,
    baseScale: Float,
    scope: CoroutineScope,
    focusRequester: FocusRequester,
    step: ((Int) -> Unit)?,
) = this
    .focusRequester(focusRequester)
    .focusable()
    .onPreviewKeyEvent { event -> event.stepPlayback(step) || viewport?.handleKey(event, baseScale, scope) == true }
    .pointerInput(focusRequester) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
            focusRequester.requestFocus()
        }
    }

private fun KeyEvent.stepPlayback(step: ((Int) -> Unit)?): Boolean {
    if (step == null || type != KeyEventType.KeyDown) return false
    step(
        when (key) {
            Key.DirectionRight -> 1
            Key.DirectionLeft -> -1
            else -> return false
        },
    )
    return true
}

private fun ScoreViewport.handleKey(event: KeyEvent, baseScale: Float, scope: CoroutineScope): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    when (event.key) {
        Key.Zero, Key.NumPad0 -> if (event.isCtrlPressed) zoomTo(1f / baseScale) else return false
        Key.PageDown, Key.DirectionDown, Key.Spacebar -> scope.launch { step(1) }
        Key.PageUp, Key.DirectionUp -> scope.launch { step(-1) }
        Key.Plus, Key.Equals, Key.NumPadAdd -> zoomBy(ScoreViewport.ZOOM_STEP)
        Key.Minus, Key.NumPadSubtract -> zoomBy(1 / ScoreViewport.ZOOM_STEP)
        else -> return false
    }
    return true
}

@Composable
private fun paperColors(): PaperColors? {
    if (!LocalDarkTheme.current) return null
    val colors = MaterialTheme.colorScheme
    return remember(colors) { PaperColors(paper = colors.surfaceContainerHighest, ink = colors.onSurfaceVariant) }
}

@Composable
private fun PersistView(viewport: ScoreViewport, part: PartRef, display: PartDisplay, viewModel: ScoreViewModel) {
    LaunchedEffect(viewport) {
        snapshotFlow { viewport.zoom to viewport.offset }.collectLatest {
            delay(SAVE_DELAY_MILLIS)
            viewModel.saveView(part, display, viewport.view)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveView(part, display, viewport.view) }
    DisposableEffect(viewport) { onDispose { viewModel.saveView(part, display, viewport.view) } }
}

@Composable
private fun ReflowOnZoom(viewport: ScoreViewport, part: PartRef, viewModel: ScoreViewModel) {
    LaunchedEffect(viewport) {
        snapshotFlow { viewport.zoom }.collectLatest { zoom ->
            if (zoom == 1f) return@collectLatest
            delay(REFLOW_DELAY_MILLIS)
            viewModel.reflow(part, zoom, viewport.view)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopToolbar(toolbar: ToolbarState) {
    TopAppBar(
        title = {
            Column {
                Text(toolbar.ready?.metadata?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                toolbar.subtitle()?.let { subtitle ->
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = { BackAction(toolbar) },
        actions = {
            PartAction(toolbar)
            InstrumentsAction(toolbar)
            PlaybackAction(toolbar)
            AnnotationAction(toolbar)
            AttachmentsMenu(toolbar.audio, toolbar.files, onOpenBar = toolbar.playback::close)
            ZoomAction(toolbar)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun SideToolbar(toolbar: ToolbarState) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                .padding(horizontal = Spacing.ExtraSmall, vertical = Spacing.Small),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            BackAction(toolbar)
            PartAction(toolbar)
            InstrumentsAction(toolbar)
            PlaybackAction(toolbar)
            AnnotationAction(toolbar)
            AttachmentsMenu(toolbar.audio, toolbar.files, onOpenBar = toolbar.playback::close)
            ZoomAction(toolbar)
        }
    }
}

@Composable
private fun BackAction(toolbar: ToolbarState) {
    IconAction(Res.drawable.ic_arrow_back, LocalStrings.current.back, onClick = toolbar.onClose)
}

@Composable
private fun ToolbarState.subtitle(): String? {
    val ready = ready ?: return null
    val strings = LocalStrings.current
    val partName = ready.parts.takeIf { it.size > 1 }?.let { parts ->
        val index = parts.indexOfFirst { it.directory == ready.part.directory }
        strings.partName(parts[index].name, index)
    }
    return listOf(ready.metadata.composer, partName.orEmpty()).filter { it.isNotEmpty() }.joinToString(SUBTITLE_SEPARATOR).ifEmpty { null }
}

@Composable
private fun ToolbarState.windowTitle(): String? =
    listOfNotNull(ready?.metadata?.title?.takeIf { it.isNotEmpty() }, subtitle()).joinToString(SUBTITLE_SEPARATOR).ifEmpty { null }

@Composable
private fun PartAction(toolbar: ToolbarState) {
    val ready = toolbar.ready?.takeIf { it.parts.size > 1 } ?: return
    val strings = LocalStrings.current
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconAction(Res.drawable.ic_piano, strings.parts, onClick = { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ready.parts.forEachIndexed { index, part ->
                val selected = part.directory == ready.part.directory
                DropdownMenuItem(
                    text = { Text(strings.partName(part.name, index), fontWeight = if (selected) FontWeight.Bold else null) },
                    leadingIcon = { if (selected) Icon(painterResource(Res.drawable.ic_check), contentDescription = null) },
                    onClick = {
                        expanded = false
                        toolbar.score.selectPart(part.directory)
                    },
                )
            }
        }
    }
}

@Composable
private fun InstrumentsAction(toolbar: ToolbarState) {
    val ready = toolbar.ready ?: return
    val instruments = ready.instruments ?: return
    val label = LocalStrings.current.firstInstrumentOnly
    LabelTooltip(label) {
        IconToggleButton(
            checked = instruments == ShownInstruments.First,
            onCheckedChange = { first -> toolbar.score.selectInstruments(ready.part, if (first) ShownInstruments.First else ShownInstruments.All) },
        ) {
            Icon(painterResource(Res.drawable.ic_filter_1), contentDescription = label)
        }
    }
}

@Composable
private fun DisplayToggle(state: ScoreViewerState.Ready, viewport: ScoreViewport, viewModel: ScoreViewModel, modifier: Modifier) {
    val strings = LocalStrings.current
    OverlayToggle(
        options = state.displays,
        selected = state.display,
        icon = { it.icon },
        label = strings::displayName,
        onSelect = { viewModel.selectDisplay(state.part, it, viewport.view) },
        modifier = modifier,
    )
}

@Composable
private fun ScrollModeToggle(mode: ScrollMode, onChange: (ScrollMode) -> Unit, modifier: Modifier) {
    val strings = LocalStrings.current
    OverlayToggle(
        options = ScrollMode.entries,
        selected = mode,
        icon = { it.icon },
        label = strings::scrollModeName,
        onSelect = onChange,
        modifier = modifier,
    )
}

@Composable
private fun <T> OverlayToggle(
    options: List<T>,
    selected: T,
    icon: (T) -> DrawableResource,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier,
) {
    val colors = SegmentedButtonDefaults.colors(inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = TOGGLE_ALPHA))
    SingleChoiceSegmentedButtonRow(modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                modifier = Modifier.size(ToggleSegmentSize),
                colors = colors,
                contentPadding = PaddingValues(),
                icon = {},
            ) {
                Icon(painterResource(icon(option)), contentDescription = label(option), modifier = Modifier.size(ToggleIconSize))
            }
        }
    }
}

private val ScrollMode.icon
    get() = when (this) {
        ScrollMode.Both -> Res.drawable.ic_open_with
        ScrollMode.Vertical -> Res.drawable.ic_height
        ScrollMode.Horizontal -> Res.drawable.ic_width
    }

private fun Strings.scrollModeName(mode: ScrollMode) = when (mode) {
    ScrollMode.Both -> scrollModeBoth
    ScrollMode.Vertical -> scrollModeVertical
    ScrollMode.Horizontal -> scrollModeHorizontal
}

@Composable
private fun StepButton(viewport: ScoreViewport, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    val label = LocalStrings.current.scrollDown
    Box(modifier) {
        LabelTooltip(label) {
            FilledTonalIconButton(
                onClick = { scope.launch { viewport.step(1) } },
                enabled = viewport.canScrollDown,
                shape = CircleShape,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = TOGGLE_ALPHA),
                ),
                modifier = Modifier.size(StepButtonSize),
            ) {
                Icon(painterResource(Res.drawable.ic_arrow_downward), contentDescription = label, modifier = Modifier.size(StepIconSize))
            }
        }
    }
}

private val PartDisplay.icon
    get() = when (this) {
        PartDisplay.Original -> Res.drawable.ic_image
        PartDisplay.Normalized -> Res.drawable.ic_contrast
        PartDisplay.MusicXml -> Res.drawable.ic_library_music
    }

private fun Strings.displayName(display: PartDisplay) = when (display) {
    PartDisplay.Original -> displayOriginal
    PartDisplay.Normalized -> displayNormalized
    PartDisplay.MusicXml -> displayMusicXml
}

@Composable
private fun PlaybackAction(toolbar: ToolbarState) {
    if (toolbar.ready?.isPlayable != true) return
    val audio by toolbar.audio.state.collectAsStateWithLifecycle()
    IconAction(
        Res.drawable.ic_music_note,
        LocalStrings.current.playback,
        onClick = {
            toolbar.audio.closePlayer()
            toolbar.playback.toggle()
        },
        enabled = toolbar.viewport != null && !audio.isRecording,
    )
}

@Composable
private fun playbackHighlight(cursor: PlaybackCursor?, notes: NotePositions?): ScoreHighlight? {
    val dark = LocalDarkTheme.current
    val colors = MaterialTheme.colorScheme
    return remember(cursor, notes, dark, colors) {
        if (cursor == null || notes == null) return@remember null
        val region = notes.cursor(cursor.notes)
        ScoreHighlight(
            cursor = region,
            notes = cursor.highlighted.mapNotNull(notes::note),
            reveal = region.takeIf { cursor.follow },
            lookahead = notes.followingSystem(cursor.notes).takeIf { cursor.follow },
            cursorColor = colors.primary.copy(alpha = CURSOR_ALPHA),
            noteColor = if (dark) colors.inversePrimary else colors.primary,
            noteBlendMode = if (dark) BlendMode.Darken else BlendMode.Lighten,
        )
    }
}

@Composable
private fun AnnotationAction(toolbar: ToolbarState) {
    IconAction(
        Res.drawable.ic_edit,
        LocalStrings.current.annotate,
        onClick = toolbar.annotations::toggleEditing,
        enabled = toolbar.viewport != null,
    )
}

@Composable
private fun ZoomAction(toolbar: ToolbarState) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconAction(Res.drawable.ic_zoom_in, LocalStrings.current.zoom, onClick = { expanded = true }, enabled = toolbar.viewport != null)
        DropdownMenu(expanded = expanded && toolbar.viewport != null, onDismissRequest = { expanded = false }) {
            toolbar.viewport?.let { ZoomSlider(it, toolbar.ready?.reflowScale ?: 1f) }
        }
    }
}

@Composable
private fun ZoomSlider(viewport: ScoreViewport, baseScale: Float) {
    val zoom = baseScale * viewport.zoom
    Row(
        modifier = Modifier.width(ZoomSliderWidth).padding(horizontal = Spacing.Large),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Slider(
            value = ln(zoom),
            onValueChange = { viewport.zoomTo(exp(it) / baseScale) },
            valueRange = ln(ScoreViewport.MIN_ZOOM)..ln(ScoreViewport.MAX_ZOOM),
            modifier = Modifier.weight(1f),
        )
        Text(
            LocalStrings.current.zoomPercent((zoom * PERCENT).roundToInt()),
            style = MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = ZoomLabelWidth),
        )
    }
}
