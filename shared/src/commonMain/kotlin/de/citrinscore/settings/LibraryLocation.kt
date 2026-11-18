package de.citrinscore.settings

import de.citrinscore.library.displayPath
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.bookmarkData
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.fromBookmarkData
import io.github.vinceglb.filekit.name
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.io.encoding.Base64

@Serializable(with = LibraryLocationSerializer::class)
sealed interface LibraryLocation {

    suspend fun resolve(): PlatformFile

    @Serializable
    data object App : LibraryLocation {
        private const val DIRECTORY = "library"

        override suspend fun resolve(): PlatformFile = (FileKit.filesDir / DIRECTORY).also { it.createDirectories() }
    }

    @Serializable
    data class Folder(
        val displayPath: String,
        val bookmark: String,
    ) : LibraryLocation {

        override suspend fun resolve(): PlatformFile = PlatformFile.fromBookmarkData(Base64.decode(bookmark))

        fun describing(directory: PlatformFile) = copy(displayPath = displayPathOf(directory))

        override fun toString() = "Folder($displayPath)"

        companion object {
            suspend fun of(directory: PlatformFile) = Folder(
                displayPath = displayPathOf(directory),
                bookmark = Base64.encode(directory.bookmarkData().bytes),
            )

            private fun displayPathOf(directory: PlatformFile) = directory.displayPath ?: directory.name
        }
    }
}

internal object LibraryLocationSerializer : JsonContentPolymorphicSerializer<LibraryLocation>(LibraryLocation::class) {
    private const val FOLDER_KEY = "bookmark"

    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<LibraryLocation> =
        if (FOLDER_KEY in element.jsonObject) LibraryLocation.Folder.serializer() else LibraryLocation.App.serializer()
}
