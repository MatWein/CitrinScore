package de.citrinscore.library

import de.citrinscore.score.CitScore
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import kotlin.time.Instant

sealed interface LibraryItem {
    val file: PlatformFile
    val displayName: String

    data class Folder(override val file: PlatformFile) : LibraryItem {
        override val displayName get() = file.name
    }

    data class Score(override val file: PlatformFile, val lastActivity: Instant?) : LibraryItem {
        override val displayName get() = CitScore.title(file.name)
    }
}

data class FolderContent(val items: List<LibraryItem>) {
    val isEmpty get() = items.isEmpty()
    val names get() = items.map { it.file.name }
    val scoreTitles get() = items.filterIsInstance<LibraryItem.Score>().map { it.displayName }

    fun arranged(sort: LibrarySort, query: String): List<LibraryItem> {
        val search = query.trim()
        val matching = items.filter { search.isEmpty() || it.displayName.contains(search, ignoreCase = true) }
        val folders = matching.filterIsInstance<LibraryItem.Folder>().sortedBy { it.displayName.lowercase() }
        val scores = matching.filterIsInstance<LibraryItem.Score>().sortedWith(sort.scoreOrder)
        return folders + scores
    }
}
