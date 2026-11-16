package de.citrinscore.library

import kotlinx.serialization.Serializable

@Serializable
enum class LibrarySort(internal val scoreOrder: Comparator<LibraryItem.Score>) {
    RecentlyUsed(compareByDescending<LibraryItem.Score> { it.lastActivity }.then(byName())),
    Title(byName()),
}

private fun byName(): Comparator<LibraryItem.Score> = compareBy { it.displayName.lowercase() }
