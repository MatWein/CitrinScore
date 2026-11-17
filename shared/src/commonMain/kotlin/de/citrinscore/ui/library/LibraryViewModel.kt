package de.citrinscore.ui.library

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.decodeToImageBitmap

private const val THUMBNAIL_CACHE_KILOBYTES = 64 * 1024
private const val BYTES_PER_PIXEL = 4
private const val BYTES_PER_KILOBYTE = 1024

class LibraryViewModel(root: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState(path = listOf(root)))
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    val currentFolder get() = _state.value.currentFolder

    var selectedScore: PlatformFile? = null
        private set

    private val thumbnails = object : LruCache<String, ImageBitmap>(THUMBNAIL_CACHE_KILOBYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * BYTES_PER_PIXEL / BYTES_PER_KILOBYTE
    }

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
            thumbnails.evictAll()
            refresh()
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun reload() {
        viewModelScope.launch {
            val existing = _state.value.path.takeWhile { attempt { it.exists() } == true }.ifEmpty { _state.value.path.take(1) }
            thumbnails.evictAll()
            _state.update { it.copy(path = existing) }
            refresh()
        }
    }

    fun refresh() {
        val folder = currentFolder
        viewModelScope.launch {
            val content = attempt { repository.content(folder) } ?: return@launch report(LibraryError.Load)
            _state.update { if (it.currentFolder == folder) it.copy(content = content) else it }
        }
    }

    fun report(error: LibraryError) = _state.update { it.copy(error = error) }

    suspend fun thumbnail(score: PlatformFile): ImageBitmap? = thumbnails[score.path]
        ?: repository.thumbnail(score)
            ?.let { bytes -> withContext(Dispatchers.Default) { bytes.decodeToImageBitmap() } }
            ?.also { thumbnails.put(score.path, it) }

    fun invalidateThumbnail(score: PlatformFile) {
        thumbnails.remove(score.path)
    }
}
