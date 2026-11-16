package de.citrinscore.ui.importer

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.render.BitmapPageSource
import de.citrinscore.score.render.PageSource
import de.citrinscore.score.render.contentCrop
import de.citrinscore.score.render.openImageSource
import de.citrinscore.score.render.openPageSource
import de.citrinscore.score.render.openPdfSource
import de.citrinscore.score.scan.ScanPage
import de.citrinscore.score.scan.scanPages
import de.citrinscore.ui.components.nextStep
import de.citrinscore.ui.components.previousStep
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val PREVIEW_WIDTH = 1000
private const val THRESHOLD_DELAY_MILLIS = 300L

class ImportViewModel(
    private val folder: PlatformFile,
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    private val scans = mutableMapOf<String, List<ScanPage>>()
    private var thresholdJob: Job? = null

    init {
        viewModelScope.launch {
            val titles = repository.content(folder).scoreTitles
            _state.update { it.copy(takenTitles = titles) }
        }
    }

    fun selectType(type: ImportType) {
        if (_state.value.type == type) return
        scans.clear()
        _state.update { it.copy(type = type, files = emptyList(), previews = emptyMap(), failedPreviews = emptySet()) }
    }

    fun addFiles(files: List<PlatformFile>) {
        val current = _state.value
        val type = current.type ?: return
        if (type == ImportType.Pdf) {
            val pdf = files.firstOrNull() ?: return
            _state.update { it.copy(files = listOf(pdf), previews = emptyMap(), failedPreviews = emptySet()) }
            loadPreview(pdf, type)
            return
        }
        val known = current.files.map { it.path }.toSet()
        val added = files.distinctBy { it.path }.filter { it.path !in known }
        _state.update { it.copy(files = it.files + added) }
        added.forEach { loadPreview(it, type) }
    }

    fun moveFile(from: Int, to: Int) = _state.update { state ->
        state.copy(files = state.files.toMutableList().apply { add(to, removeAt(from)) })
    }

    fun removeFile(index: Int) {
        val path = _state.value.files[index].path
        scans.remove(path)
        _state.update { state ->
            state.copy(
                files = state.files.filterIndexed { i, _ -> i != index },
                previews = state.previews - path,
                failedPreviews = state.failedPreviews - path,
            )
        }
    }

    fun updateScanThreshold(threshold: Float) {
        _state.update { it.copy(scanThreshold = threshold) }
        thresholdJob?.cancel()
        thresholdJob = viewModelScope.launch {
            delay(THRESHOLD_DELAY_MILLIS)
            scans.toMap().forEach { (path, pages) ->
                val previews = attempt { pages.map { it.preview(threshold) } } ?: return@forEach
                _state.update { state -> if (path in state.previews) state.copy(previews = state.previews + (path to previews)) else state }
            }
        }
    }

    fun updateTitle(title: String) = _state.update { it.copy(title = title) }

    fun updateComposer(composer: String) = _state.update { it.copy(composer = composer) }

    fun updateDescription(description: String) = _state.update { it.copy(description = description) }

    fun back() = _state.update { it.copy(step = it.step.previousStep()) }

    fun next(onImported: () -> Unit) {
        val current = _state.value
        if (!current.canAdvance) return
        when (current.step) {
            ImportStep.Metadata -> save(current, onImported)
            else -> _state.update { it.copy(step = it.step.nextStep()) }
        }
    }

    private fun loadPreview(file: PlatformFile, type: ImportType) {
        viewModelScope.launch {
            val pages = attempt {
                type.open(file).use { source ->
                    if (type == ImportType.Scan) {
                        source.scanPages().also { scans[file.path] = it }.map { it.preview(_state.value.scanThreshold) }
                    } else {
                        source.pageSizes.indices.map { page -> source.previewPage(page) }
                    }
                }
            }
            _state.update { state ->
                when {
                    state.files.none { it.path == file.path } -> state
                    pages == null -> state.copy(failedPreviews = state.failedPreviews + file.path)
                    else -> state.copy(previews = state.previews + (file.path to pages))
                }
            }
        }
    }

    private suspend fun ScanPage.preview(threshold: Float) = BitmapPageSource(binarized(threshold)).use { it.previewPage(0) }

    private suspend fun PageSource.previewPage(page: Int): PreviewPage {
        val crop = contentCrop(page)
        val size = IntSize(PREVIEW_WIDTH, (PREVIEW_WIDTH * crop.size.height / crop.size.width).roundToInt().coerceAtLeast(1))
        return PreviewPage(withContext(Dispatchers.Default) { render(page, crop.rect, size) }, crop)
    }

    private fun save(current: ImportState, onImported: () -> Unit) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            val saved = attempt {
                val metadata = ScoreMetadata(current.title.trim(), current.composer.trim(), current.description.trim())
                val crops = current.pages.map { it.crop }
                when (current.type) {
                    ImportType.Images -> repository.importImages(folder, metadata, current.files, crops)
                    ImportType.Scan -> saveScans(current, metadata)
                    ImportType.Pdf -> repository.importPdf(folder, metadata, current.files.single(), crops)
                    null -> error("No import type selected")
                }
            }
            if (saved == null) _state.update { it.copy(isSaving = false, saveFailed = true) } else onImported()
        }
    }

    private suspend fun saveScans(current: ImportState, metadata: ScoreMetadata) {
        val (images, crops) = current.files.flatMap { scans.getValue(it.path) }.map { page ->
            val image = page.binarized(current.scanThreshold)
            val crop = BitmapPageSource(image).use { it.contentCrop(0) }
            withContext(Dispatchers.Default) { image.encodeToByteArray(ImageFormat.PNG) } to crop
        }.unzip()
        repository.importScans(folder, metadata, images, crops)
    }

    private suspend fun ImportType.open(file: PlatformFile): PageSource = when (this) {
        ImportType.Images -> openImageSource(file)
        ImportType.Scan -> openPageSource(file)
        ImportType.Pdf -> openPdfSource(file)
    }
}
