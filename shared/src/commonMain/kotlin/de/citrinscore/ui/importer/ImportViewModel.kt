package de.citrinscore.ui.importer

import androidx.compose.ui.unit.IntSize
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.render.PageSource
import de.citrinscore.score.render.contentCrop
import de.citrinscore.score.render.openImageSource
import de.citrinscore.score.render.openPdfSource
import de.citrinscore.ui.components.nextStep
import de.citrinscore.ui.components.previousStep
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private const val PREVIEW_WIDTH = 1000

class ImportViewModel(
    private val folder: PlatformFile,
    private val repository: LibraryRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ImportState())
    val state: StateFlow<ImportState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val titles = repository.content(folder).scoreTitles
            _state.update { it.copy(takenTitles = titles) }
        }
    }

    fun selectType(type: ImportType) = _state.update { state ->
        if (state.type == type) state else state.copy(type = type, files = emptyList(), previews = emptyMap(), failedPreviews = emptySet())
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

    fun removeFile(index: Int) = _state.update { state ->
        val path = state.files[index].path
        state.copy(
            files = state.files.filterIndexed { i, _ -> i != index },
            previews = state.previews - path,
            failedPreviews = state.failedPreviews - path,
        )
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
            val pages = try {
                type.open(file).use { source -> source.pageSizes.indices.map { page -> source.previewPage(page) } }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
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

    private suspend fun PageSource.previewPage(page: Int): PreviewPage {
        val crop = contentCrop(page)
        val size = IntSize(PREVIEW_WIDTH, (PREVIEW_WIDTH * crop.size.height / crop.size.width).roundToInt().coerceAtLeast(1))
        return PreviewPage(withContext(Dispatchers.Default) { render(page, crop.rect, size) }, crop)
    }

    private fun save(current: ImportState, onImported: () -> Unit) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val metadata = ScoreMetadata(current.title.trim(), current.composer.trim(), current.description.trim())
                val crops = current.pages.map { it.crop }
                when (current.type) {
                    ImportType.Images -> repository.importImages(folder, metadata, current.files, crops)
                    ImportType.Pdf -> repository.importPdf(folder, metadata, current.files.single(), crops)
                    null -> error("Unsupported import type ${current.type}")
                }
                onImported()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }

    private suspend fun ImportType.open(file: PlatformFile): PageSource = when (this) {
        ImportType.Images -> openImageSource(file)
        ImportType.Pdf -> openPdfSource(file)
    }
}
