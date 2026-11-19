package de.citrinscore.ui.importer

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import de.citrinscore.library.NewPart
import de.citrinscore.library.PartSource
import de.citrinscore.score.PageCrop
import de.citrinscore.score.scan.EraserStroke
import de.citrinscore.score.render.BitmapPageSource
import de.citrinscore.score.render.PageSource
import de.citrinscore.score.render.contentCrop
import de.citrinscore.score.render.openImageSource
import de.citrinscore.score.render.openMusicXmlSource
import de.citrinscore.score.render.openPageSource
import de.citrinscore.score.render.openPdfSource
import de.citrinscore.score.render.scanSize
import de.citrinscore.score.omr.musicXmlPreview
import de.citrinscore.score.omr.recognizeMusicXml
import de.citrinscore.score.scan.ScanPage
import de.citrinscore.score.scan.scanPages
import de.citrinscore.ui.pages.previewPage
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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private const val THRESHOLD_DELAY_MILLIS = 300L
private const val OMR_DELAY_MILLIS = 500L
private const val OMR_RESOLUTION = 2000f
private const val OMR_PART_SUFFIX = "OMR"
private val logger = LoggerFactory.getLogger(PartDraftEditor::class.java)

class PartDraftEditor(private val scope: CoroutineScope) {

    private val _draft = MutableStateFlow(PartDraft())
    val draft: StateFlow<PartDraft> = _draft.asStateFlow()

    private val scans = mutableMapOf<String, List<ScanPage>>()
    private var thresholdJob: Job? = null

    init {
        scope.launch {
            _draft.map { draft -> draft.takeIf { it.isOmrRequested && it.isPreviewReady }?.let { it.key to it.omrInput } }
                .distinctUntilChanged()
                .collectLatest { request ->
                    val (key, input) = request ?: return@collectLatest
                    val finished = _draft.value.omr?.takeIf { it !is OmrState.Running }
                    if (finished?.input == input) return@collectLatest
                    delay(OMR_DELAY_MILLIS)
                    recognize(key, input)
                }
        }
    }

    fun edit(draft: PartDraft) {
        thresholdJob?.cancel()
        _draft.value = draft
    }

    fun discard(draft: PartDraft) {
        scans.keys.removeAll { it.startsWith(scanKeyPrefix(draft)) }
    }

    fun selectType(type: ImportType) {
        val current = _draft.value
        if (current.type == type) return
        logger.info("Selecting import type {}", type)
        discard(current)
        _draft.value = current.copy(type = type, files = emptyList(), previews = emptyMap(), failedPreviews = emptySet(), scans = emptyMap(), erasedPages = emptyList(), omr = null)
    }

    fun updateName(name: String) = _draft.update { it.copy(name = name) }

    fun enableOmr(enabled: Boolean) = _draft.update { draft ->
        draft.copy(omrEnabled = enabled, omr = draft.omr as? OmrState.Done)
    }

    fun addFiles(files: List<PlatformFile>) {
        val current = _draft.value
        val type = current.type ?: return
        if (type.singleFile) {
            val file = files.firstOrNull() ?: return
            _draft.value = current.copy(files = listOf(file), previews = emptyMap(), failedPreviews = emptySet(), scans = emptyMap(), erasedPages = emptyList())
            loadPreview(current.key, file, type)
            return
        }
        val known = current.files.map { it.path }.toSet()
        val added = files.distinctBy { it.path }.filter { it.path !in known }
        _draft.value = current.copy(files = current.files + added)
        added.forEach { loadPreview(current.key, it, type) }
    }

    fun moveFile(from: Int, to: Int) = _draft.update { draft ->
        draft.copy(files = draft.files.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun removeFile(index: Int) {
        val current = _draft.value
        val path = current.files[index].path
        _draft.value = current.copy(
            files = current.files.filterIndexed { i, _ -> i != index },
            previews = current.previews - path,
            failedPreviews = current.failedPreviews - path,
            scans = current.scans - path,
            erasedPages = current.erasedPages.filterNot { it.path == path },
        )
    }

    fun cropScan(page: ScanPageRef, crop: PageCrop) = _draft.update { draft -> draft.withScan(page) { it.copy(crop = crop) } }

    fun eraseScan(page: ScanPageRef, stroke: EraserStroke) = _draft.update { draft ->
        draft.withScan(page) { it.copy(erased = it.erased + stroke) }.copy(erasedPages = draft.erasedPages + page)
    }

    fun undoErase() = _draft.update { draft ->
        val page = draft.erasedPages.lastOrNull() ?: return@update draft
        draft.withScan(page) { it.copy(erased = it.erased.dropLast(1)) }.copy(erasedPages = draft.erasedPages.dropLast(1))
    }

    fun updateScanThreshold(threshold: Float) {
        val key = _draft.value.key
        _draft.update { it.copy(scanThreshold = threshold) }
        thresholdJob?.cancel()
        thresholdJob = scope.launch {
            delay(THRESHOLD_DELAY_MILLIS)
            _draft.value.files.forEach { file ->
                val pages = scans[scanKey(key, file.path)] ?: return@forEach
                val images = attempt { pages.map { it.previewImage(it.binarized(threshold)) } } ?: return@forEach
                _draft.update { draft ->
                    val current = draft.scans[file.path]
                    if (draft.key != key || current == null) {
                        draft
                    } else {
                        draft.copy(scans = draft.scans + (file.path to current.zip(images) { scan, image -> scan.copy(image = image) }))
                    }
                }
            }
        }
    }

    suspend fun newParts(draft: PartDraft): List<NewPart> {
        val name = draft.name.trim()
        val recognized = draft.recognized ?: return listOf(NewPart(name, source(draft)))
        val omrName = if (name.isEmpty()) OMR_PART_SUFFIX else "$name ($OMR_PART_SUFFIX)"
        return listOf(NewPart(name, source(draft)), NewPart(omrName, PartSource.RecognizedMusicXml(recognized.musicXml)))
    }

    private suspend fun recognize(key: String, input: OmrInput) {
        logger.info("Recognizing the music of part {}", key)
        publishOmr(key, input, OmrState.Running(input, 0f))
        val result = attempt {
            val musicXml = recognizeMusicXml(omrPages(_draft.value)) { progress -> publishOmr(key, input, OmrState.Running(input, progress)) }
            OmrState.Done(input, musicXml, musicXmlPreview(musicXml))
        }
        publishOmr(key, input, result ?: OmrState.Failed(input))
    }

    private fun publishOmr(key: String, input: OmrInput, state: OmrState) = _draft.update { draft ->
        if (draft.key == key && draft.isOmrRequested && draft.omrInput == input) draft.copy(omr = state) else draft
    }

    private suspend fun omrPages(draft: PartDraft): List<ImageBitmap> = when (draft.type) {
        ImportType.Scan -> draft.scanPages.map { (page, scan) ->
            val image = scans.getValue(scanKey(draft.key, page.path))[page.index].grayscale(scan.erased)
            withContext(Dispatchers.Default) { BitmapPageSource(image).use { it.render(0, scan.crop.rect, scanSize(scan.crop.size, 1f)) } }
        }
        ImportType.Images, ImportType.Pdf -> draft.files.flatMap { file ->
            draft.type.open(file).use { source ->
                draft.previews.getValue(file.path).mapIndexed { page, preview ->
                    val crop = preview.crop
                    withContext(Dispatchers.Default) { source.render(page, crop.rect, scanSize(crop.size, OMR_RESOLUTION / crop.size.width)) }
                }
            }
        }
        ImportType.MusicXml, null -> error("Music recognition is not supported for ${draft.type}")
    }

    suspend fun source(draft: PartDraft): PartSource {
        val crops = draft.pages.map { it.crop }
        return when (draft.type) {
            ImportType.Images -> PartSource.Images(draft.files, crops)
            ImportType.Pdf -> PartSource.Pdf(draft.files.single(), crops)
            ImportType.Scan -> scanSource(draft)
            ImportType.MusicXml -> PartSource.MusicXml(draft.files.single())
            null -> error("No import type selected")
        }
    }

    private suspend fun scanSource(draft: PartDraft): PartSource {
        val (images, crops) = draft.scanPages.map { (page, scan) ->
            val image = scans.getValue(scanKey(draft.key, page.path))[page.index].binarized(draft.scanThreshold, scan.erased)
            val crop = BitmapPageSource(image).use { it.contentCrop(0, within = scan.crop) }
            withContext(Dispatchers.Default) { image.encodeToByteArray(ImageFormat.PNG) } to crop
        }.unzip()
        return PartSource.Scans(images, crops)
    }

    private fun loadPreview(key: String, file: PlatformFile, type: ImportType) {
        logger.info("Loading {} preview of {}", type, file.path)
        scope.launch {
            if (type == ImportType.Scan) {
                val scanned = attempt { type.open(file).use { it.scanPages() }.also { scans[scanKey(key, file.path)] = it }.map { it.scanPreview() } }
                publishPreview(key, file, scanned?.let { pages -> { copy(scans = scans + (file.path to pages)) } })
            } else {
                val pages = attempt { type.open(file).use { source -> source.pageSizes.indices.map { page -> source.previewPage(page) } } }
                publishPreview(key, file, pages?.let { loaded -> { copy(previews = previews + (file.path to loaded)) } })
            }
        }
    }

    private fun publishPreview(key: String, file: PlatformFile, loaded: (PartDraft.() -> PartDraft)?) = _draft.update { draft ->
        when {
            draft.key != key || draft.files.none { it.path == file.path } -> draft
            loaded == null -> draft.copy(failedPreviews = draft.failedPreviews + file.path)
            else -> draft.loaded()
        }
    }

    private suspend fun ScanPage.scanPreview(): ScanPreview {
        val image = binarized(_draft.value.scanThreshold)
        val crop = BitmapPageSource(image).use { it.contentCrop(0) }
        return ScanPreview(previewImage(image), size, crop)
    }

    private suspend fun ScanPage.previewImage(image: ImageBitmap) = withContext(Dispatchers.Default) {
        BitmapPageSource(image).use { it.render(0, Rect(Offset.Zero, size), previewSize(size.width, size.height)) }
    }

    private suspend fun ImportType.open(file: PlatformFile): PageSource = when (this) {
        ImportType.Images -> openImageSource(file)
        ImportType.Scan -> openPageSource(file)
        ImportType.Pdf -> openPdfSource(file)
        ImportType.MusicXml -> openMusicXmlSource(file)
    }

    private fun scanKeyPrefix(draft: PartDraft) = "${draft.key}/"

    private fun scanKey(draftKey: String, path: String) = "$draftKey/$path"
}
