package de.citrinscore.ui.library

import androidx.collection.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.library.TransferMode
import de.citrinscore.library.showInFileManager
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
import org.slf4j.LoggerFactory

private const val THUMBNAIL_CACHE_KILOBYTES = 64 * 1024
private const val BYTES_PER_PIXEL = 4
private const val BYTES_PER_KILOBYTE = 1024
private val logger = LoggerFactory.getLogger(LibraryViewModel::class.java)

class LibraryViewModel(root: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState(path = listOf(root)))
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    val currentFolder get() = _state.value.currentFolder

    var selectedScore: PlatformFile? = null
        private set

    /** The folder holding [selectedScore], which is not the current one for a score found by a search. */
    var selectedFolder: PlatformFile? = null
        private set

    private var searching = false

    private val thumbnails = object : LruCache<String, ImageBitmap>(THUMBNAIL_CACHE_KILOBYTES) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * BYTES_PER_PIXEL / BYTES_PER_KILOBYTE
    }

    init {
        refresh()
    }

    /** Opens [folder], or the last of several nested folders below the current one. */
    fun open(vararg folder: PlatformFile) {
        logger.info("Opening folder {}", folder.last().path)
        searching = false
        _state.update { it.copy(path = it.path + folder, content = null, selection = emptySet(), searchEntries = null) }
        refresh()
    }

    fun up() {
        if (_state.value.isRoot) return
        logger.info("Leaving folder {}", currentFolder.path)
        searching = false
        _state.update { it.copy(path = it.path.dropLast(1), content = null, selection = emptySet(), searchEntries = null) }
        refresh()
    }

    fun createFolder(name: String) {
        val parent = currentFolder
        viewModelScope.launch {
            attempt { repository.createFolder(parent, name.trim()) } ?: report(LibraryError.CreateFolder)
            refresh()
        }
    }

    fun select(score: PlatformFile, folder: PlatformFile = currentFolder) {
        selectedScore = score
        selectedFolder = folder
    }

    /** Loads everything below the current folder while a search is open, so the search reaches into subfolders. */
    fun setSearching(active: Boolean) {
        if (active == searching) return
        searching = active
        if (active) loadSearchEntries() else _state.update { it.copy(searchEntries = null) }
    }

    private fun loadSearchEntries() {
        val folder = currentFolder
        viewModelScope.launch {
            val entries = attempt { repository.entries(folder) } ?: return@launch report(LibraryError.Load)
            _state.update { if (searching && it.currentFolder == folder) it.copy(searchEntries = entries) else it }
        }
    }

    fun setSelection(selection: Set<String>) = _state.update { it.copy(selection = selection) }

    fun clearSelection() = setSelection(emptySet())

    /** Drops selected items that are no longer shown, for example because a search hides them. */
    fun retainSelection(visible: Set<String>) = _state.update { it.copy(selection = it.selection intersect visible) }

    fun delete(items: List<PlatformFile>) {
        logger.info("Deleting {} items", items.size)
        clearSelection()
        viewModelScope.launch {
            val failed = items.count { item -> attempt { repository.delete(item) } == null }
            if (failed > 0) report(LibraryError.Delete)
            thumbnails.evictAll()
            refresh()
        }
    }

    fun transfer(items: List<PlatformFile>, target: PlatformFile, mode: TransferMode) {
        val source = currentFolder
        clearSelection()
        viewModelScope.launch {
            val failed = attempt { repository.transfer(items, source, target, mode) } ?: items
            if (failed.isNotEmpty()) report(if (mode == TransferMode.Move) LibraryError.Move else LibraryError.Copy)
            thumbnails.evictAll()
            refresh()
        }
    }

    fun export(score: PlatformFile, target: PlatformFile) {
        viewModelScope.launch {
            attempt { repository.exportScore(score, target) } ?: report(LibraryError.Export)
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun reload() {
        viewModelScope.launch {
            val existing = _state.value.path.takeWhile { attempt { it.exists() } == true }.ifEmpty { _state.value.path.take(1) }
            thumbnails.evictAll()
            _state.update { it.copy(path = existing, selection = emptySet()) }
            refresh()
        }
    }

    fun showInFileManager(folder: PlatformFile) {
        viewModelScope.launch {
            attempt { folder.showInFileManager() } ?: report(LibraryError.ShowInFileManager)
        }
    }

    fun refresh() {
        val folder = currentFolder
        viewModelScope.launch {
            val content = attempt { repository.content(folder) } ?: return@launch report(LibraryError.Load)
            _state.update { if (it.currentFolder == folder) it.copy(content = content) else it }
        }
        if (searching) loadSearchEntries()
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
