package de.citrinscore.ui.library

import de.citrinscore.library.FolderContent
import io.github.vinceglb.filekit.PlatformFile

data class LibraryState(
    val path: List<PlatformFile>,
    val content: FolderContent? = null,
    val deleteFailed: Boolean = false,
) {
    val currentFolder get() = path.last()
    val isRoot get() = path.size == 1
}
