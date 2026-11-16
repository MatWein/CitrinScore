package de.citrinscore.settings

import de.citrinscore.library.displayPath
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.name
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

@Serializable
data class LibraryReference(
    val displayPath: String,
    val bookmark: String,
) {
    suspend fun resolve(): PlatformFile = PlatformFile.fromBookmarkData(Base64.decode(bookmark))

    fun describing(directory: PlatformFile) = copy(displayPath = displayPathOf(directory))

    companion object {
        suspend fun of(directory: PlatformFile) = LibraryReference(
            displayPath = displayPathOf(directory),
            bookmark = Base64.encode(directory.bookmarkData().bytes),
        )

        private fun displayPathOf(directory: PlatformFile) = directory.displayPath ?: directory.name
    }
}
