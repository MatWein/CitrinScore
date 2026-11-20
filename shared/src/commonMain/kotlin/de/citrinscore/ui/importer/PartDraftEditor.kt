package de.citrinscore.ui.importer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.library.NewNormalization
import de.citrinscore.library.NewPage
import de.citrinscore.library.NewPart
import de.citrinscore.library.PageOrigin
import de.citrinscore.library.PartSource
import de.citrinscore.score.CitScore
import de.citrinscore.score.PageCrop
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScorePart
import de.citrinscore.score.omr.RecognitionPage
import de.citrinscore.score.render.BitmapPageSource
import de.citrinscore.score.render.contentCrop
import de.citrinscore.score.render.openPageSource
import de.citrinscore.score.render.scanSize
import de.citrinscore.score.scan.DEFAULT_SCAN_THRESHOLD
import de.citrinscore.score.scan.EraserStroke
import de.citrinscore.score.scan.ScanPage
import de.citrinscore.score.scan.scanPage
import de.citrinscore.ui.pages.PreviewPage
import de.citrinscore.ui.pages.previewSize
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private const val THRESHOLD_DELAY_MILLIS = 300L
private val logger = LoggerFactory.getLogger(PartDraftEditor::class.java)

class PartDraftEditor(private val scope: CoroutineScope) {

    private val _draft = MutableStateFlow(PartDraft())
    val draft: StateFlow<PartDraft> = _draft.asStateFlow()

    private val scanPages = mutableMapOf<String, ScanPage>()
    private var thresholdJob: Job? = null

    init {
        scope.launchMusicRecognition(
            requests = _draft.map { draft -> draft.takeIf { it.isOmrRequested && it.isPreviewReady }?.omrInput },
            current = { _draft.value.omr },
            pages = { recognitionPages(_draft.value) },
            publish = { input, state -> _draft.update { draft -> if (draft.isOmrRequested && draft.omrInput == input) draft.copy(omr = state) else draft } },
        )
    }

    fun edit(draft: PartDraft) {
        thresholdJob?.cancel()
        _draft.value = draft
        draft.pages.filter { it.view == null && !it.failed }.forEach { loadPage(draft.key, it.key) }
    }

    fun load(root: PlatformFile, part: ScorePart, loadRecognition: suspend () -> List<PreviewPage>?) {
        val draft = when (val content = part.content) {
            is ScoreContent.MusicXml -> PartDraft(name = part.name, musicXml = DraftMusicXml(CitScore.resolve(root, content.path), stored = true))
            is ScoreContent.Pages -> PartDraft(
                name = part.name,
                pages = content.pages.mapIndexed { index, page ->
                    DraftPage(
                        file = CitScore.resolve(root, page.source),
                        page = page.page,
                        pageCount = content.pages.count { it.source == page.source },
                        storedPath = page.source,
                        previousIndex = index,
                        crop = page.crop,
                        erased = page.normalization.erased,
                    )
                },
                threshold = content.pages.firstOrNull()?.normalization?.threshold ?: DEFAULT_SCAN_THRESHOLD,
                omrEnabled = part.recognition != null,
            )
        }
        val stored = draft.takeIf { part.recognition != null }?.let { StoredRecognition(it.omrInput) }
        edit(draft.copy(storedRecognition = stored))
        if (stored == null) return
        scope.launch {
            val preview = attempt { loadRecognition() } ?: return@launch
            _draft.update { current -> if (current.key == draft.key) current.copy(storedRecognition = current.storedRecognition?.copy(preview = preview)) else current }
        }
    }

    fun discard(draft: PartDraft) {
        draft.pages.forEach { scanPages.remove(it.key) }
    }

    fun updateName(name: String) = _draft.update { it.copy(name = name) }

    fun enableOmr(enabled: Boolean) = _draft.update { draft -> draft.copy(omrEnabled = enabled, omr = draft.omr as? OmrState.Done) }

    fun addFiles(files: List<PlatformFile>) {
        val key = _draft.value.key
        val musicXml = files.firstOrNull { SourceKind.of(it) == SourceKind.MusicXml }
        if (musicXml != null) {
            logger.info("Using MusicXML {}", musicXml.path)
            discard(_draft.value)
            _draft.update { it.copy(pages = emptyList(), musicXml = DraftMusicXml(musicXml), erasedPages = emptyList(), omr = null) }
            return
        }
        scope.launch {
            files.filter { SourceKind.of(it) != null }.forEach { file ->
                logger.info("Adding pages of {}", file.path)
                val count = attempt { openPageSource(file).use { it.pageSizes.size } }
                val added = if (count == null) {
                    listOf(DraftPage(file, 0, 1, failed = true))
                } else {
                    List(count) { page -> DraftPage(file, page, count) }
                }
                _draft.update { draft -> if (draft.key == key) draft.copy(pages = draft.pages + added, musicXml = null) else draft }
                added.filterNot { it.failed }.forEach { loadPage(key, it.key) }
            }
        }
    }

    fun movePage(from: Int, to: Int) = _draft.update { draft -> draft.copy(pages = draft.pages.toMutableList().apply { add(to, removeAt(from)) }) }

    fun removePage(index: Int) = _draft.update { draft ->
        val page = draft.pages[index]
        scanPages.remove(page.key)
        draft.copy(pages = draft.pages.filterIndexed { i, _ -> i != index }, erasedPages = draft.erasedPages.filterNot { it == page.key })
    }

    fun removeMusicXml() = _draft.update { it.copy(musicXml = null) }

    fun cropPage(key: String, crop: PageCrop) = _draft.update { draft -> draft.withPage(key) { it.copy(crop = crop) } }

    fun erasePage(key: String, stroke: EraserStroke) = _draft.update { draft ->
        draft.withPage(key) { it.copy(erased = it.erased + stroke) }.copy(erasedPages = draft.erasedPages + key)
    }

    fun undoErase() = _draft.update { draft ->
        val key = draft.erasedPages.lastOrNull() ?: return@update draft
        draft.withPage(key) { it.copy(erased = it.erased.dropLast(1)) }.copy(erasedPages = draft.erasedPages.dropLast(1))
    }

    fun updateThreshold(threshold: Float) {
        val key = _draft.value.key
        _draft.update { it.copy(threshold = threshold) }
        thresholdJob?.cancel()
        thresholdJob = scope.launch {
            delay(THRESHOLD_DELAY_MILLIS)
            _draft.value.pages.forEach { page ->
                val scan = scanPages[page.key] ?: return@forEach
                val image = attempt { previewImage(scan.binarized(threshold), scan) } ?: return@forEach
                _draft.update { draft -> if (draft.key == key) draft.withPage(page.key) { it.copy(image = image) } else draft }
            }
        }
    }

    suspend fun newPart(draft: PartDraft) = NewPart(draft.name.trim(), checkNotNull(source(draft)), draft.recognized?.music)

    suspend fun source(draft: PartDraft): PartSource? {
        val musicXml = draft.musicXml
        if (musicXml != null) return PartSource.MusicXml(musicXml.file).takeUnless { musicXml.stored }
        return PartSource.Pages(draft.pages.map { page -> newPage(draft, page) })
    }

    private suspend fun newPage(draft: PartDraft, page: DraftPage): NewPage {
        val origin = page.storedPath?.let(PageOrigin::Stored) ?: PageOrigin.File(page.file)
        val image = scanPages.getValue(page.key).binarized(draft.threshold, page.erased)
        val normalization = NewNormalization(withContext(Dispatchers.Default) { image.encodeToByteArray(ImageFormat.PNG) }, draft.threshold, page.erased)
        return NewPage(origin, page.page, checkNotNull(page.crop), normalization, page.previousIndex)
    }

    private suspend fun recognitionPages(draft: PartDraft): List<RecognitionPage> = draft.pages.map { page ->
        val crop = checkNotNull(page.crop)
        val image = scanPages.getValue(page.key).grayscale(page.erased)
        RecognitionPage(withContext(Dispatchers.Default) { BitmapPageSource(image).use { it.render(0, crop.rect, scanSize(crop.size, 1f)) } }, 1f)
    }

    private fun loadPage(draftKey: String, pageKey: String) {
        val page = _draft.value.pages.firstOrNull { it.key == pageKey } ?: return
        scope.launch {
            val loaded = attempt {
                val scan = scanPages[page.key] ?: openPageSource(page.file).use { it.scanPage(page.page) }.also { scanPages[page.key] = it }
                val binarized = scan.binarized(_draft.value.threshold)
                val crop = page.crop ?: BitmapPageSource(binarized).use { it.contentCrop(0) }
                page.copy(image = previewImage(binarized, scan), size = scan.size, crop = crop)
            }
            _draft.update { draft ->
                val current = draft.pages.firstOrNull { it.key == pageKey }
                when {
                    draft.key != draftKey || current == null -> draft
                    loaded == null -> draft.withPage(pageKey) { it.copy(failed = true) }
                    else -> draft.withPage(pageKey) { it.copy(image = loaded.image, size = loaded.size, crop = it.crop ?: loaded.crop) }
                }
            }
        }
    }

    private suspend fun previewImage(image: ImageBitmap, scan: ScanPage) = withContext(Dispatchers.Default) {
        BitmapPageSource(image).use { it.render(0, Rect(Offset.Zero, scan.size), previewSize(scan.size.width, scan.size.height)) }
    }
}
