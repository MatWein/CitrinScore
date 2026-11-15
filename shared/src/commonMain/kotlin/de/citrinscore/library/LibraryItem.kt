package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.nameWithoutExtension

sealed interface LibraryItem {
    val file: PlatformFile
    val displayName: String

    data class Folder(override val file: PlatformFile) : LibraryItem {
        override val displayName get() = file.name
    }

    data class Score(override val file: PlatformFile) : LibraryItem {
        override val displayName get() = file.nameWithoutExtension
    }
}

data class FolderContent(val items: List<LibraryItem>) {
    val isEmpty get() = items.isEmpty()
    val names get() = items.map { it.file.name }
    val scoreTitles get() = items.filterIsInstance<LibraryItem.Score>().map { it.displayName }
}
