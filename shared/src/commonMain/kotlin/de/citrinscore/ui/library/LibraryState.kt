package de.citrinscore.ui.library

import de.citrinscore.library.FolderContent
import de.citrinscore.library.LibraryItem
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

enum class LibraryError { Load, CreateFolder, Delete, Export, Move, Copy, SaveSettings }

data class LibraryState(
    val path: List<PlatformFile>,
    val content: FolderContent? = null,
    val error: LibraryError? = null,
    val selection: Set<String> = emptySet(),
) {
    val currentFolder get() = path.last()
    val parentFolder get() = path.getOrNull(path.lastIndex - 1)
    val isRoot get() = path.size == 1
    val isSelecting get() = selection.isNotEmpty()

    fun isSelected(item: LibraryItem) = item.file.path in selection
}
