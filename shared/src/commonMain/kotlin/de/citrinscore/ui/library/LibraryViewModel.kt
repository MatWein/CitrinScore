package de.citrinscore.ui.library

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
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
            repository.createFolder(parent, name.trim())
            refresh()
        }
    }

    fun refresh() {
        val folder = currentFolder
        viewModelScope.launch {
            val content = repository.content(folder)
            _state.update { if (it.currentFolder == folder) it.copy(content = content) else it }
        }
    }

    suspend fun thumbnail(score: PlatformFile): ImageBitmap? =
        repository.thumbnail(score)?.let { bytes -> withContext(Dispatchers.Default) { bytes.decodeToImageBitmap() } }
}
