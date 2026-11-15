package de.citrinscore.settings

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.path
import kotlinx.serialization.Serializable
import kotlin.io.encoding.Base64

@Serializable
data class LibraryReference(
    val displayPath: String,
    val bookmark: String,
) {
    suspend fun resolve(): PlatformFile = PlatformFile.fromBookmarkData(Base64.decode(bookmark))

    companion object {
        suspend fun of(directory: PlatformFile) = LibraryReference(
            displayPath = directory.path,
            bookmark = Base64.encode(directory.bookmarkData().bytes),
        )
    }
}
