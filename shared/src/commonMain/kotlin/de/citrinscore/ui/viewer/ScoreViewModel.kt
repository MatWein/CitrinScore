package de.citrinscore.ui.viewer

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.DisplayRef
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.PartRef
import de.citrinscore.score.CitScore
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScorePart
import de.citrinscore.score.PartDisplay
import de.citrinscore.score.PlaybackPreferences
import de.citrinscore.score.RecognizedMusic
import de.citrinscore.score.Recognition
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.ScrollMode
import de.citrinscore.score.musicxml.EngravedPage
import de.citrinscore.score.musicxml.EngravingLayout
import de.citrinscore.score.musicxml.MeasureAnchors
import de.citrinscore.score.musicxml.MusicXmlEngraver
import de.citrinscore.score.musicxml.NotePositions
import de.citrinscore.score.musicxml.ScorePerformance
import de.citrinscore.score.musicxml.notePositions
import de.citrinscore.score.musicxml.openMusicXmlEngraver
import de.citrinscore.score.render.PageSourceRenderer
import de.citrinscore.score.render.ScoreRenderer
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.pageSource
import de.citrinscore.score.render.openMusicXmlEngraver
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import kotlin.math.roundToInt

private const val UNITS_PER_DP = 1.65f
private const val MIN_PAGE_SIZE = 100
private const val MAX_PAGE_SIZE = 60000
private const val DEFAULT_REFLOW_SCALE = 1f
private const val MIN_LAYOUT_WIDTH = 1100f
private const val VIEWPORT_SETTLE_MILLIS = 150L
private val logger = LoggerFactory.getLogger(ScoreViewModel::class.java)

class ScoreViewModel(private val score: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow<ScoreViewerState>(ScoreViewerState.Loading)
    val state: StateFlow<ScoreViewerState> = _state.asStateFlow()

    private val _scrollMode = MutableStateFlow(ScrollMode.Vertical)
    val scrollMode: StateFlow<ScrollMode> = _scrollMode.asStateFlow()

    private val _part = MutableStateFlow<PartRef?>(null)
    val part: StateFlow<PartRef?> = _part.asStateFlow()

    private val _display = MutableStateFlow<DisplayRef?>(null)
    val display: StateFlow<DisplayRef?> = _display.asStateFlow()

    private val _anchors = MutableStateFlow<MeasureAnchors?>(null)
    val anchors: StateFlow<MeasureAnchors?> = _anchors.asStateFlow()

    private val _notes = MutableStateFlow<NotePositions?>(null)
    val notes: StateFlow<NotePositions?> = _notes.asStateFlow()

    private val viewportSize = MutableStateFlow<Size?>(null)
    private val renderers = mutableMapOf<Pair<String, PartDisplay>, ScoreRenderer>()
    private val engravers = mutableMapOf<String, MusicXmlEngraver>()
    private val recognitions = mutableMapOf<String, RecognizedMusic>()
    private val performances = mutableMapOf<String, ScorePerformance>()
    private var manifest: ScoreManifest? = null
    private val showing = Mutex()
    private val opening = Mutex()

    init {
        viewModelScope.launch {
            val opened = attempt { repository.open(score) } ?: return@launch run { _state.value = ScoreViewerState.Failed }
            manifest = opened
            opened.scrollMode?.let { _scrollMode.value = it }
            val current = opened.currentPart?.takeIf { directory -> opened.parts.any { it.directory == directory } }
            show(current ?: opened.parts.first().directory)
        }
        viewModelScope.launch {
            viewportSize.filterNotNull().collectLatest {
                delay(VIEWPORT_SETTLE_MILLIS)
                val directory = _part.value?.directory ?: return@collectLatest
                if (manifest?.part(directory)?.shownDisplay?.isEngraved == true) show(directory)
            }
        }
    }

    fun updateViewportSize(width: Float, height: Float) {
        viewportSize.value = Size(width, height)
    }

    fun selectPart(directory: String) {
        if (_part.value?.directory == directory) return
        viewModelScope.launch {
            show(directory)
            attempt { repository.selectPart(PartRef(score, directory)) }
        }
    }

    fun selectScrollMode(mode: ScrollMode) {
        if (_scrollMode.value == mode) return
        _scrollMode.value = mode
        manifest = manifest?.copy(scrollMode = mode)
        viewModelScope.launch {
            withContext(NonCancellable) { attempt { repository.saveScrollMode(score, mode) } }
        }
    }

    fun selectDisplay(part: PartRef, display: PartDisplay, view: ScoreViewState?) {
        val current = manifest ?: return
        val shown = current.part(part.directory).shownDisplay
        if (shown == display) return
        logger.info("Showing {} as {}", part, display)
        saveView(part, shown, view)
        manifest = manifest?.updatePart(part.directory) { it.copy(display = display) }
        viewModelScope.launch {
            show(part.directory)
            withContext(NonCancellable) { attempt { repository.saveDisplay(part, display) } }
        }
    }

    fun reflow(part: PartRef, zoom: Float, view: ScoreViewState?) {
        val current = manifest ?: return
        val scale = (current.part(part.directory).reflowScale * zoom).coerceIn(ScoreViewport.MIN_ZOOM, ScoreViewport.MAX_ZOOM)
        logger.info("Reflowing {} at scale {}", part, scale)
        val reflowed = ScoreViewState(scale, view?.x ?: 0f, view?.y ?: 0f)
        manifest = current.updatePart(part.directory) { it.copy(engravedView = reflowed) }
        persistView(part, PartDisplay.MusicXml, reflowed)
        viewModelScope.launch { show(part.directory) }
    }

    suspend fun performance(part: PartRef): ScorePerformance? = attempt {
        performances[part.directory] ?: engraver(part, manifest?.part(part.directory) ?: return@attempt null)?.perform()?.also { performances[part.directory] = it }
    }

    suspend fun writtenPitches(part: PartRef, notes: Collection<String>): Map<String, Int> =
        attempt { manifest?.part(part.directory)?.let { engraver(part, it) }?.writtenPitches(notes) }.orEmpty()

    suspend fun render(part: PartRef, display: PartDisplay, page: Int, region: Rect, size: IntSize): ImageBitmap? =
        attempt { renderers[part.directory to display]?.render(page, region, size) }

    fun saveView(part: PartRef, display: PartDisplay, view: ScoreViewState?) {
        val current = manifest ?: return
        val scorePart = current.part(part.directory)
        val stored = (if (display.isEngraved) view?.copy(zoom = scorePart.reflowScale) else view) ?: return
        if (scorePart.view(display) == stored) return
        manifest = current.updatePart(part.directory) { it.withView(display, stored) }
        persistView(part, display, stored)
    }

    fun playback(part: PartRef): PlaybackPreferences? = manifest?.part(part.directory)?.playback

    fun savePlayback(part: PartRef, playback: PlaybackPreferences) {
        val current = manifest ?: return
        if (current.part(part.directory).playback == playback) return
        manifest = current.updatePart(part.directory) { it.copy(playback = playback) }
        viewModelScope.launch {
            withContext(NonCancellable) { attempt { repository.savePlayback(part, playback) } }
        }
    }

    private fun persistView(part: PartRef, display: PartDisplay, view: ScoreViewState) {
        viewModelScope.launch {
            withContext(NonCancellable) { attempt { repository.saveView(part, display, view) } }
        }
    }

    private suspend fun show(directory: String) = showing.withLock { present(directory) }

    private suspend fun present(directory: String) {
        val current = manifest ?: return
        val part = current.part(directory)
        val display = part.shownDisplay
        val ref = PartRef(score, directory)
        if (_part.value != ref) {
            _anchors.value = null
            _notes.value = null
        }
        _part.value = ref
        _display.value = DisplayRef(ref, display)
        logger.info("Showing {} as {}", ref, display)
        val recognized = part.recognition?.let { attempt { recognition(ref, it) } }
        var engraved: List<EngravedPage>? = null
        val renderer = if (display.isEngraved) {
            val size = viewportSize.value ?: return run { _state.value = ScoreViewerState.Loading }
            attempt { engrave(ref, part, layout(size, part.reflowScale)) }?.let { (renderer, pages) ->
                engraved = pages
                renderer
            }
        } else {
            attempt { renderers.getOrPut(directory to display) { ScoreRenderers.open(ref.root, part.content, display) } }
        } ?: return run { _state.value = ScoreViewerState.Failed }
        if (_part.value != ref) return
        _anchors.value = engraved?.let { pages -> MeasureAnchors(pages.map { it.staves }) }
        _notes.value = engraved?.let(::NotePositions) ?: recognized?.layout?.notePositions(renderer.pageSizes.size)
        _state.value = ScoreViewerState.Ready(
            metadata = current.metadata,
            parts = current.parts,
            part = ref,
            display = display,
            displays = part.displays,
            pageSizes = renderer.pageSizes,
            view = if (display.isEngraved) part.engravedView?.copy(zoom = DEFAULT_REFLOW_SCALE) else part.view,
            reflowScale = part.reflowScale.takeIf { display.isEngraved },
            isPlayable = engraved != null || _notes.value != null,
        )
    }

    private suspend fun engrave(part: PartRef, scorePart: ScorePart, layout: EngravingLayout): Pair<ScoreRenderer, List<EngravedPage>> {
        val pages = checkNotNull(engraver(part, scorePart)).engrave(layout)
        val renderer = PageSourceRenderer(pages.pageSource())
        renderers.put(part.directory to PartDisplay.MusicXml, renderer)?.close()
        return renderer to pages
    }

    private suspend fun engraver(part: PartRef, scorePart: ScorePart): MusicXmlEngraver? = opening.withLock {
        engravers[part.directory] ?: when (val content = scorePart.content) {
            is ScoreContent.MusicXml -> openMusicXmlEngraver(CitScore.resolve(part.root, content.path))
            is ScoreContent.Pages -> scorePart.recognition?.let { recognition(part, it) }?.let { openMusicXmlEngraver(it.musicXml.encodeToByteArray(), compressed = false) }
        }?.also { engravers[part.directory] = it }
    }

    private suspend fun recognition(part: PartRef, recognition: Recognition): RecognizedMusic? =
        recognitions[part.directory] ?: repository.recognition(part, recognition)?.also { recognitions[part.directory] = it }

    private fun layout(viewport: Size, scale: Float): EngravingLayout {
        val width = maxOf(viewport.width * UNITS_PER_DP, MIN_LAYOUT_WIDTH) / scale
        return EngravingLayout(
            pageWidth = width.roundToInt().coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
            pageHeight = (width * viewport.height / viewport.width).roundToInt().coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
        )
    }

    private val ScorePart.reflowScale get() = engravedView?.zoom ?: DEFAULT_REFLOW_SCALE

    override fun onCleared() {
        renderers.values.forEach { it.close() }
        engravers.values.forEach { it.close() }
    }
}
