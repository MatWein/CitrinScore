package de.citrinscore.ui.importer

import de.citrinscore.library.PartSource
import de.citrinscore.score.render.BitmapPageSource
import de.citrinscore.score.render.PageSource
import de.citrinscore.score.render.contentCrop
import de.citrinscore.score.render.openImageSource
import de.citrinscore.score.render.openMusicXmlSource
import de.citrinscore.score.render.openPageSource
import de.citrinscore.score.render.openPdfSource
import de.citrinscore.score.scan.ScanPage
import de.citrinscore.score.scan.scanPages
import de.citrinscore.ui.pages.previewPage
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory

private const val THRESHOLD_DELAY_MILLIS = 300L
private val logger = LoggerFactory.getLogger(PartDraftEditor::class.java)

class PartDraftEditor(private val scope: CoroutineScope) {

    private val _draft = MutableStateFlow(PartDraft())
    val draft: StateFlow<PartDraft> = _draft.asStateFlow()

    private val scans = mutableMapOf<String, List<ScanPage>>()
    private var thresholdJob: Job? = null

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
        _draft.value = current.copy(type = type, files = emptyList(), previews = emptyMap(), failedPreviews = emptySet())
    }

    fun updateName(name: String) = _draft.update { it.copy(name = name) }

    fun addFiles(files: List<PlatformFile>) {
        val current = _draft.value
        val type = current.type ?: return
        if (type.singleFile) {
            val file = files.firstOrNull() ?: return
            _draft.value = current.copy(files = listOf(file), previews = emptyMap(), failedPreviews = emptySet())
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
        )
    }

    fun updateScanThreshold(threshold: Float) {
        val key = _draft.value.key
        _draft.update { it.copy(scanThreshold = threshold) }
        thresholdJob?.cancel()
        thresholdJob = scope.launch {
            delay(THRESHOLD_DELAY_MILLIS)
            _draft.value.files.forEach { file ->
                val pages = scans[scanKey(key, file.path)] ?: return@forEach
                val previews = attempt { pages.map { it.preview(threshold) } } ?: return@forEach
                _draft.update { draft ->
                    if (draft.key == key && file.path in draft.previews) draft.copy(previews = draft.previews + (file.path to previews)) else draft
                }
            }
        }
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
        val (images, crops) = draft.files.flatMap { scans.getValue(scanKey(draft.key, it.path)) }.map { page ->
            val image = page.binarized(draft.scanThreshold)
            val crop = BitmapPageSource(image).use { it.contentCrop(0) }
            withContext(Dispatchers.Default) { image.encodeToByteArray(ImageFormat.PNG) } to crop
        }.unzip()
        return PartSource.Scans(images, crops)
    }

    private fun loadPreview(key: String, file: PlatformFile, type: ImportType) {
        logger.info("Loading {} preview of {}", type, file.path)
        scope.launch {
            val pages = attempt {
                type.open(file).use { source ->
                    if (type == ImportType.Scan) {
                        source.scanPages().also { scans[scanKey(key, file.path)] = it }.map { it.preview(_draft.value.scanThreshold) }
                    } else {
                        source.pageSizes.indices.map { page -> source.previewPage(page) }
                    }
                }
            }
            _draft.update { draft ->
                when {
                    draft.key != key || draft.files.none { it.path == file.path } -> draft
                    pages == null -> draft.copy(failedPreviews = draft.failedPreviews + file.path)
                    else -> draft.copy(previews = draft.previews + (file.path to pages))
                }
            }
        }
    }

    private suspend fun ScanPage.preview(threshold: Float) = BitmapPageSource(binarized(threshold)).use { it.previewPage(0) }

    private suspend fun ImportType.open(file: PlatformFile): PageSource = when (this) {
        ImportType.Images -> openImageSource(file)
        ImportType.Scan -> openPageSource(file)
        ImportType.Pdf -> openPdfSource(file)
        ImportType.MusicXml -> openMusicXmlSource(file)
    }

    private fun scanKeyPrefix(draft: PartDraft) = "${draft.key}/"

    private fun scanKey(draftKey: String, path: String) = "$draftKey/$path"
}
