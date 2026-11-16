package de.citrinscore.ui.library

import de.citrinscore.library.FolderContent
import io.github.vinceglb.filekit.PlatformFile

enum class LibraryError { Load, CreateFolder, Delete, SaveSettings }

data class LibraryState(
    val path: List<PlatformFile>,
    val content: FolderContent? = null,
    val error: LibraryError? = null,
) {
    val currentFolder get() = path.last()
    val isRoot get() = path.size == 1
}
