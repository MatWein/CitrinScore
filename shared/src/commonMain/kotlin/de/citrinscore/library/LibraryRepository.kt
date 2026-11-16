package de.citrinscore.library

import de.citrinscore.score.CitScore
import de.citrinscore.score.ImagePage
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScoreThumbnail
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.render.contentCrop
import de.citrinscore.serialization.AppJson
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.write
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class LibraryRepository(
    private val json: Json = AppJson,
    private val clock: Clock = Clock.System,
) {

    suspend fun content(folder: PlatformFile): FolderContent = withContext(Dispatchers.IO) {
        val directories = folder.list().filter { !it.name.startsWith(".") && it.isDirectory() }
        val (scoreDirectories, subfolders) = directories.partition { it.isScore() }
        val folders = subfolders
            .sortedBy { it.name.lowercase() }
            .map(LibraryItem::Folder)
        val scores = scoreDirectories
            .map { LibraryItem.Score(it, manifest(it)?.lastActivity) }
            .sortedWith(compareByDescending<LibraryItem.Score> { it.lastActivity }.thenBy { it.displayName.lowercase() })
        FolderContent(folders + scores)
    }

    suspend fun createFolder(parent: PlatformFile, name: String) {
        (parent / name).createDirectories()
    }

    suspend fun delete(item: PlatformFile) = withContext(Dispatchers.IO) {
        item.delete(mustExist = false, recursively = true)
        if (item.exists()) throw IOException("${item.name} could not be deleted")
    }

    suspend fun open(score: PlatformFile): ScoreManifest {
        val manifest = requireManifest(score)
        val opened = manifest.copy(content = manifest.content.withCrops(score), lastOpenedAt = clock.now())
        writeManifest(score, opened)
        return opened
    }

    suspend fun saveView(score: PlatformFile, view: ScoreViewState) {
        writeManifest(score, requireManifest(score).copy(view = view))
    }

    suspend fun importImages(folder: PlatformFile, metadata: ScoreMetadata, images: List<PlatformFile>) {
        val score = folder / CitScore.directoryName(metadata.title)
        val pageNames = images.mapIndexed { index, image -> CitScore.pageFileName(index, image.extension) }
        val crops = images.map { contentCrop(it) }
        val manifest = ScoreManifest(
            metadata = metadata,
            content = ScoreContent.Images(pageNames.zip(crops) { name, crop -> ImagePage("${CitScore.PAGES_DIRECTORY}/$name", crop) }),
            createdAt = clock.now(),
        )

        try {
            val pages = score / CitScore.PAGES_DIRECTORY
            pages.createDirectories()
            images.zip(pageNames).forEach { (image, name) -> (pages / name) write image }
            (score / CitScore.THUMBNAIL_FILE) write ScoreThumbnail.render(images, crops)
            writeManifest(score, manifest)
        } catch (e: Exception) {
            score.delete(mustExist = false, recursively = true)
            throw e
        }
    }

    suspend fun thumbnail(score: PlatformFile): ByteArray? = read(score / CitScore.THUMBNAIL_FILE) { readBytes() }

    private suspend fun ScoreContent.withCrops(score: PlatformFile): ScoreContent = when (this) {
        is ScoreContent.Images -> copy(pages = pages.map { page -> page.crop?.let { page } ?: page.copy(crop = contentCrop(CitScore.resolve(score, page.path))) })
    }

    private suspend fun requireManifest(score: PlatformFile) = manifest(score) ?: throw IOException("${score.name} has no readable manifest")

    private suspend fun writeManifest(score: PlatformFile, manifest: ScoreManifest) {
        (score / CitScore.MANIFEST_FILE).writeString(json.encodeToString(manifest))
    }

    private suspend fun manifest(score: PlatformFile): ScoreManifest? = read(score / CitScore.MANIFEST_FILE) { readString() }
        ?.let { text ->
            try {
                json.decodeFromString<ScoreManifest>(text)
            } catch (_: IllegalArgumentException) {
                null
            }
        }

    private suspend fun <T> read(file: PlatformFile, block: suspend PlatformFile.() -> T): T? = try {
        if (file.exists()) file.block() else null
    } catch (_: IOException) {
        null
    }

    private fun PlatformFile.isScore() = CitScore.isScoreDirectoryName(name)
}
