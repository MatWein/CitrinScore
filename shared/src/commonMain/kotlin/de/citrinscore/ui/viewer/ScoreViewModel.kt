package de.citrinscore.ui.viewer

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.PartRef
import de.citrinscore.score.CitScore
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScorePart
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.isReflowable
import de.citrinscore.score.musicxml.EngravingLayout
import de.citrinscore.score.musicxml.MeasureAnchors
import de.citrinscore.score.musicxml.MusicXmlEngraver
import de.citrinscore.score.render.PageSourceRenderer
import de.citrinscore.score.render.ScoreRenderer
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.pageSource
import de.citrinscore.score.render.openMusicXmlEngraver
import de.citrinscore.score.render.systemBreaks
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val UNITS_PER_DP = 1.65f
private const val MIN_PAGE_SIZE = 100
private const val MAX_PAGE_SIZE = 60000
private const val DEFAULT_REFLOW_SCALE = 1f
private const val MIN_LAYOUT_WIDTH = 1100f
private const val VIEWPORT_SETTLE_MILLIS = 150L

class ScoreViewModel(private val score: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow<ScoreViewerState>(ScoreViewerState.Loading)
    val state: StateFlow<ScoreViewerState> = _state.asStateFlow()

    private val _systemBreaks = MutableStateFlow(emptyList<List<Float>>())
    val systemBreaks: StateFlow<List<List<Float>>> = _systemBreaks.asStateFlow()

    private val _part = MutableStateFlow<PartRef?>(null)
    val part: StateFlow<PartRef?> = _part.asStateFlow()

    private val _anchors = MutableStateFlow<MeasureAnchors?>(null)
    val anchors: StateFlow<MeasureAnchors?> = _anchors.asStateFlow()

    private val viewportSize = MutableStateFlow<Size?>(null)
    private val renderers = mutableMapOf<String, ScoreRenderer>()
    private val engravers = mutableMapOf<String, MusicXmlEngraver>()
    private var manifest: ScoreManifest? = null
    private var detection: Job? = null

    init {
        viewModelScope.launch {
            val opened = attempt { repository.open(score) } ?: return@launch run { _state.value = ScoreViewerState.Failed }
            manifest = opened
            val current = opened.currentPart?.takeIf { directory -> opened.parts.any { it.directory == directory } }
            show(current ?: opened.parts.first().directory)
        }
        viewModelScope.launch {
            viewportSize.filterNotNull().collectLatest {
                delay(VIEWPORT_SETTLE_MILLIS)
                val directory = _part.value?.directory ?: return@collectLatest
                if (manifest?.part(directory)?.content?.isReflowable == true) show(directory)
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

    fun reflow(part: PartRef, zoom: Float, view: ScoreViewState?) {
        val current = manifest ?: return
        val scale = (current.part(part.directory).reflowScale * zoom).coerceIn(ScoreViewport.MIN_ZOOM, ScoreViewport.MAX_ZOOM)
        val reflowed = ScoreViewState(scale, view?.x ?: 0f, view?.y ?: 0f)
        manifest = current.updatePart(part.directory) { it.copy(view = reflowed) }
        persistView(part, reflowed)
        viewModelScope.launch { show(part.directory) }
    }

    suspend fun render(part: PartRef, page: Int, region: Rect, size: IntSize): ImageBitmap? =
        attempt { renderers[part.directory]?.render(page, region, size) }

    fun saveView(part: PartRef, view: ScoreViewState?) {
        val current = manifest ?: return
        val scorePart = current.part(part.directory)
        val stored = (if (scorePart.content.isReflowable) view?.copy(zoom = scorePart.reflowScale) else view) ?: return
        if (scorePart.view == stored) return
        manifest = current.updatePart(part.directory) { it.copy(view = stored) }
        persistView(part, stored)
    }

    private fun persistView(part: PartRef, view: ScoreViewState) {
        viewModelScope.launch {
            withContext(NonCancellable) { attempt { repository.saveView(part, view) } }
        }
    }

    private suspend fun show(directory: String) {
        val current = manifest ?: return
        val part = current.part(directory)
        val ref = PartRef(score, directory)
        detection?.cancel()
        if (_part.value != ref) _anchors.value = null
        _part.value = ref
        val content = part.content
        var anchors: MeasureAnchors? = null
        val renderer = if (content is ScoreContent.MusicXml) {
            val size = viewportSize.value ?: return run { _state.value = ScoreViewerState.Loading }
            attempt { engrave(ref, content, layout(size, part.reflowScale)) }?.let { (renderer, engravedAnchors) ->
                anchors = engravedAnchors
                renderer
            }
        } else {
            attempt { renderers.getOrPut(directory) { ScoreRenderers.open(ref.root, content) } }
        } ?: return run { _state.value = ScoreViewerState.Failed }
        if (_part.value != ref) return
        _anchors.value = anchors
        val breaks = part.systemBreaks?.takeIf { !content.isReflowable && it.size == renderer.pageSizes.size }
        _systemBreaks.value = breaks.orEmpty()
        _state.value = ScoreViewerState.Ready(
            metadata = current.metadata,
            parts = current.parts,
            part = ref,
            pageSizes = renderer.pageSizes,
            view = if (content.isReflowable) part.view?.copy(zoom = DEFAULT_REFLOW_SCALE) else part.view,
            reflowScale = part.reflowScale.takeIf { content.isReflowable },
        )
        if (breaks == null) detection = viewModelScope.launch { detectSystemBreaks(ref, renderer, persist = !content.isReflowable) }
    }

    private suspend fun engrave(part: PartRef, content: ScoreContent.MusicXml, layout: EngravingLayout): Pair<ScoreRenderer, MeasureAnchors> {
        val engraver = engravers.getOrPut(part.directory) { openMusicXmlEngraver(CitScore.resolve(part.root, content.path)) }
        val pages = engraver.engrave(layout)
        val renderer = PageSourceRenderer(pages.pageSource())
        renderers.put(part.directory, renderer)?.close()
        return renderer to MeasureAnchors(pages.map { it.staves })
    }

    private fun layout(viewport: Size, scale: Float): EngravingLayout {
        val width = maxOf(viewport.width * UNITS_PER_DP, MIN_LAYOUT_WIDTH) / scale
        return EngravingLayout(
            pageWidth = width.roundToInt().coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
            pageHeight = (width * viewport.height / viewport.width).roundToInt().coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE),
        )
    }

    private val ScorePart.reflowScale get() = view?.zoom ?: DEFAULT_REFLOW_SCALE

    private suspend fun detectSystemBreaks(part: PartRef, renderer: ScoreRenderer, persist: Boolean) {
        for (page in renderer.pageSizes.indices) {
            val breaks = attempt { renderer.systemBreaks(page) } ?: return
            _systemBreaks.update { it + listOf(breaks) }
        }
        if (!persist) return
        val detected = _systemBreaks.value
        manifest = manifest?.updatePart(part.directory) { it.copy(systemBreaks = detected) }
        attempt { repository.saveSystemBreaks(part, detected) }
    }

    override fun onCleared() {
        renderers.values.forEach { it.close() }
        engravers.values.forEach { it.close() }
    }
}
