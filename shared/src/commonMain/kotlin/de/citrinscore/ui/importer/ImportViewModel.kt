package de.citrinscore.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.ui.components.nextStep
import de.citrinscore.ui.components.previousStep
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.util.toImageBitmap
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

    fun selectType(type: ImportType) = _state.update { it.copy(type = type) }

    fun addFiles(files: List<PlatformFile>) {
        val known = _state.value.files.map { it.path }.toSet()
        val added = files.distinctBy { it.path }.filter { it.path !in known }
        _state.update { it.copy(files = it.files + added) }
        added.forEach(::loadPreview)
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

    private fun loadPreview(file: PlatformFile) {
        viewModelScope.launch {
            val preview = try {
                withContext(Dispatchers.Default) { file.toImageBitmap() }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                null
            }
            _state.update { state ->
                when {
                    state.files.none { it.path == file.path } -> state
                    preview == null -> state.copy(failedPreviews = state.failedPreviews + file.path)
                    else -> state.copy(previews = state.previews + (file.path to preview))
                }
            }
        }
    }

    private fun save(current: ImportState, onImported: () -> Unit) {
        _state.update { it.copy(isSaving = true, saveFailed = false) }
        viewModelScope.launch {
            try {
                val metadata = ScoreMetadata(current.title.trim(), current.composer.trim(), current.description.trim())
                repository.importImages(folder, metadata, current.files)
                onImported()
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isSaving = false, saveFailed = true) }
            }
        }
    }
}
