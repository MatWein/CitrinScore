package de.citrinscore.ui.library

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

class LibraryViewModel(root: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState(path = listOf(root)))
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    val currentFolder get() = _state.value.currentFolder

    var selectedScore: PlatformFile? = null
        private set

    init {
        refresh()
    }

    fun open(folder: PlatformFile) {
        _state.update { it.copy(path = it.path + folder, content = null) }
        refresh()
    }

    fun up() {
        if (_state.value.isRoot) return
        _state.update { it.copy(path = it.path.dropLast(1), content = null) }
        refresh()
    }

    fun createFolder(name: String) {
        val parent = currentFolder
        viewModelScope.launch {
            attempt { repository.createFolder(parent, name.trim()) } ?: report(LibraryError.CreateFolder)
            refresh()
        }
    }

    fun select(score: PlatformFile) {
        selectedScore = score
    }

    fun delete(item: PlatformFile) {
        viewModelScope.launch {
            attempt { repository.delete(item) } ?: report(LibraryError.Delete)
            refresh()
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun refresh() {
        val folder = currentFolder
        viewModelScope.launch {
            val content = attempt { repository.content(folder) } ?: return@launch report(LibraryError.Load)
            _state.update { if (it.currentFolder == folder) it.copy(content = content) else it }
        }
    }

    private fun report(error: LibraryError) = _state.update { it.copy(error = error) }

    suspend fun thumbnail(score: PlatformFile): ImageBitmap? =
        repository.thumbnail(score)?.let { bytes -> withContext(Dispatchers.Default) { bytes.decodeToImageBitmap() } }
}
