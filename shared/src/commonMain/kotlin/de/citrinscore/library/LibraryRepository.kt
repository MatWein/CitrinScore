package de.citrinscore.library

import de.citrinscore.archive.ArchiveEntry
import de.citrinscore.archive.writeZipArchive
import de.citrinscore.score.CitScore
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.serialization.AppJson
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.isRegularFile
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

class LibraryRepository(private val json: Json = AppJson) {

    suspend fun content(folder: PlatformFile): FolderContent = withContext(Dispatchers.IO) {
        val entries = folder.list().filterNot { it.name.startsWith(".") }.sortedBy { it.name.lowercase() }
        val folders = entries.filter { it.isDirectory() }.map(LibraryItem::Folder)
        val scores = entries
            .filter { it.isRegularFile() && it.extension.equals(CitScore.EXTENSION, ignoreCase = true) }
            .map(LibraryItem::Score)
        FolderContent(folders + scores)
    }

    suspend fun createFolder(parent: PlatformFile, name: String) {
        (parent / name).createDirectories()
    }

    suspend fun importImages(folder: PlatformFile, metadata: ScoreMetadata, images: List<PlatformFile>) {
        val pages = images.mapIndexed { index, image -> CitScore.pageEntry(index, image.extension) to image }
        val manifest = ScoreManifest(metadata, ScoreContent.Images(pages.map { (entry, _) -> entry }))
        val entries = listOf(ArchiveEntry(CitScore.MANIFEST_ENTRY) { json.encodeToString(manifest).encodeToByteArray() }) +
            pages.map { (entry, image) -> ArchiveEntry(entry) { image.readBytes() } }

        val target = folder / CitScore.fileName(metadata.title)
        try {
            target.writeZipArchive(entries)
        } catch (e: Exception) {
            target.delete(mustExist = false)
            throw e
        }
    }
}
