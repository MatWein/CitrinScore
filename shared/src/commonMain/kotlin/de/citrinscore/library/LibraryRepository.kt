package de.citrinscore.library

import de.citrinscore.score.Attachment
import de.citrinscore.score.AttachmentKind
import de.citrinscore.score.CitScore
import de.citrinscore.score.ImagePage
import de.citrinscore.score.PageCrop
import de.citrinscore.score.PdfPage
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScoreThumbnail
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.contentCrop
import de.citrinscore.score.render.openImageSource
import de.citrinscore.score.render.systemBreaks
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
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.uuid.Uuid

class LibraryRepository(
    private val json: Json = AppJson,
    private val clock: Clock = Clock.System,
) {

    private val manifestUpdates = Mutex()

    suspend fun content(folder: PlatformFile): FolderContent = withContext(Dispatchers.IO) {
        val directories = folder.list().filter { !it.name.startsWith(".") && it.isDirectory() }
        val (scoreDirectories, subfolders) = directories.partition { it.isScore() }
        val folders = subfolders.map(LibraryItem::Folder)
        val scores = scoreDirectories.map { LibraryItem.Score(it, manifest(it)?.lastActivity) }
        FolderContent(folders + scores)
    }

    suspend fun createFolder(parent: PlatformFile, name: String) {
        (parent / name).createDirectories()
    }

    suspend fun delete(item: PlatformFile) = withContext(Dispatchers.IO) {
        item.delete(mustExist = false, recursively = true)
        if (item.exists()) throw IOException("${item.name} could not be deleted")
    }

    suspend fun open(score: PlatformFile): ScoreManifest = updateManifest(score) { manifest ->
        manifest.copy(content = manifest.content.withCrops(score), lastOpenedAt = clock.now())
    }

    suspend fun load(score: PlatformFile): ScoreManifest = readManifest(score).let { it.copy(content = it.content.withCrops(score)) }

    suspend fun updateScore(folder: PlatformFile, score: PlatformFile, metadata: ScoreMetadata, pages: List<PageEdit>): PlatformFile {
        val previous = load(score)
        val content = when (val current = previous.content) {
            is ScoreContent.Images -> ScoreContent.Images(
                pages.map { page ->
                    when (page) {
                        is PageEdit.Keep -> current.pages[page.index]
                        is PageEdit.Add -> addPage(score, page)
                    }
                },
            )
            is ScoreContent.Pdf -> ScoreContent.Pdf(
                current.path,
                pages.map { page ->
                    require(page is PageEdit.Keep) { "Pages cannot be added to a PDF score" }
                    current.pages[page.index]
                },
            )
        }
        val systemBreaks = writeDerivedData(score, content)
        updateManifest(score) { manifest ->
            manifest.copy(
                metadata = metadata,
                content = content,
                systemBreaks = systemBreaks,
                view = manifest.view.takeIf { content == previous.content },
            )
        }
        removedImages(previous.content, content).forEach { CitScore.resolve(score, it.path).delete(mustExist = false) }
        val newIndexByOldIndex = pages.withIndex().mapNotNull { (index, page) -> (page as? PageEdit.Keep)?.let { it.index to index } }.toMap()
        if ((score / CitScore.ANNOTATIONS_FILE).exists()) saveAnnotations(score, annotations(score).remapPages(newIndexByOldIndex))
        return if (metadata.title == previous.metadata.title) score else score.renameDirectory(folder, CitScore.directoryName(metadata.title))
    }

    suspend fun annotations(score: PlatformFile): ScoreAnnotations =
        read(score / CitScore.ANNOTATIONS_FILE) { readString() }?.let { decode<ScoreAnnotations>(it) } ?: ScoreAnnotations()

    suspend fun saveAnnotations(score: PlatformFile, annotations: ScoreAnnotations) {
        (score / CitScore.ANNOTATIONS_FILE).writeString(json.encodeToString(annotations))
    }

    suspend fun saveView(score: PlatformFile, view: ScoreViewState) {
        updateManifest(score) { it.copy(view = view) }
    }

    suspend fun saveSystemBreaks(score: PlatformFile, systemBreaks: List<List<Float>>) {
        updateManifest(score) { it.copy(systemBreaks = systemBreaks) }
    }

    suspend fun attachments(score: PlatformFile, kind: AttachmentKind): List<Attachment> = manifest(score)?.attachments(kind).orEmpty()

    suspend fun addAttachment(score: PlatformFile, kind: AttachmentKind, source: PlatformFile, name: String): Attachment {
        val suffix = source.extension.takeIf { it.isNotEmpty() }?.let { ".${it.lowercase()}" }.orEmpty()
        val attachment = Attachment("${kind.directory}/${Uuid.random()}$suffix", name)
        (score / kind.directory).createDirectories()
        CitScore.resolve(score, attachment.path) write source
        updateManifest(score) { it.withAttachments(kind, it.attachments(kind) + attachment) }
        return attachment
    }

    suspend fun renameAttachment(score: PlatformFile, kind: AttachmentKind, attachment: Attachment, name: String) {
        updateManifest(score) { manifest ->
            manifest.withAttachments(kind, manifest.attachments(kind).map { if (it.path == attachment.path) it.copy(name = name) else it })
        }
    }

    suspend fun deleteAttachment(score: PlatformFile, kind: AttachmentKind, attachment: Attachment) {
        updateManifest(score) { manifest -> manifest.withAttachments(kind, manifest.attachments(kind).filterNot { it.path == attachment.path }) }
        CitScore.resolve(score, attachment.path).delete(mustExist = false)
    }

    suspend fun exportAttachment(score: PlatformFile, attachment: Attachment, target: PlatformFile) {
        target write CitScore.resolve(score, attachment.path)
    }

    suspend fun importImages(folder: PlatformFile, metadata: ScoreMetadata, images: List<PlatformFile>, crops: List<PageCrop>) =
        importPages(folder, metadata, images.map { it.extension }, crops) { index, target -> target write images[index] }

    suspend fun importScans(folder: PlatformFile, metadata: ScoreMetadata, scans: List<ByteArray>, crops: List<PageCrop>) =
        importPages(folder, metadata, scans.map { CitScore.SCAN_EXTENSION }, crops) { index, target -> target.write(scans[index]) }

    private suspend fun importPages(
        folder: PlatformFile,
        metadata: ScoreMetadata,
        extensions: List<String>,
        crops: List<PageCrop>,
        writePage: suspend (index: Int, target: PlatformFile) -> Unit,
    ) {
        val pageNames = extensions.mapIndexed { index, extension -> CitScore.pageFileName(index, extension) }
        val content = ScoreContent.Images(pageNames.zip(crops) { name, crop -> ImagePage("${CitScore.PAGES_DIRECTORY}/$name", crop) })
        createScore(folder, metadata, content) { score ->
            val pages = score / CitScore.PAGES_DIRECTORY
            pages.createDirectories()
            pageNames.forEachIndexed { index, name -> writePage(index, pages / name) }
        }
    }

    suspend fun importPdf(folder: PlatformFile, metadata: ScoreMetadata, pdf: PlatformFile, crops: List<PageCrop>) =
        createScore(folder, metadata, ScoreContent.Pdf(CitScore.PDF_FILE, crops.mapIndexed(::PdfPage))) { score -> (score / CitScore.PDF_FILE) write pdf }

    suspend fun thumbnail(score: PlatformFile): ByteArray? = read(score / CitScore.THUMBNAIL_FILE) { readBytes() }

    private suspend fun createScore(
        folder: PlatformFile,
        metadata: ScoreMetadata,
        content: ScoreContent,
        writeContent: suspend (PlatformFile) -> Unit,
    ) {
        val score = folder / CitScore.directoryName(metadata.title)
        try {
            score.createDirectories()
            writeContent(score)
            val systemBreaks = writeDerivedData(score, content)
            writeManifest(score, ScoreManifest(metadata, content, createdAt = clock.now(), systemBreaks = systemBreaks))
        } catch (e: Exception) {
            score.delete(mustExist = false, recursively = true)
            throw e
        }
    }

    private fun ScoreManifest.attachments(kind: AttachmentKind) = when (kind) {
        AttachmentKind.Audio -> audio
        AttachmentKind.File -> files
    }

    private fun ScoreManifest.withAttachments(kind: AttachmentKind, attachments: List<Attachment>) = when (kind) {
        AttachmentKind.Audio -> copy(audio = attachments)
        AttachmentKind.File -> copy(files = attachments)
    }

    private suspend fun writeDerivedData(score: PlatformFile, content: ScoreContent): List<List<Float>> =
        ScoreRenderers.open(score, content).use { renderer ->
            (score / CitScore.THUMBNAIL_FILE) write ScoreThumbnail.render(renderer)
            renderer.pageSizes.indices.map { renderer.systemBreaks(it) }
        }

    private suspend fun addPage(score: PlatformFile, page: PageEdit.Add): ImagePage {
        val path = "${CitScore.PAGES_DIRECTORY}/${Uuid.random()}.${page.image.extension.lowercase()}"
        (score / CitScore.PAGES_DIRECTORY).createDirectories()
        CitScore.resolve(score, path) write page.image
        return ImagePage(path, page.crop)
    }

    private fun removedImages(previous: ScoreContent, current: ScoreContent): List<ImagePage> {
        if (previous !is ScoreContent.Images || current !is ScoreContent.Images) return emptyList()
        val kept = current.pages.map { it.path }.toSet()
        return previous.pages.filterNot { it.path in kept }
    }

    private suspend fun ScoreContent.withCrops(score: PlatformFile): ScoreContent = when (this) {
        is ScoreContent.Images -> copy(pages = pages.map { page -> page.crop?.let { page } ?: page.copy(crop = imageCrop(CitScore.resolve(score, page.path))) })
        is ScoreContent.Pdf -> this
    }

    private suspend fun imageCrop(image: PlatformFile) = openImageSource(image).use { it.contentCrop(0) }

    private suspend fun updateManifest(score: PlatformFile, transform: suspend (ScoreManifest) -> ScoreManifest) = manifestUpdates.withLock {
        transform(readManifest(score)).also { writeManifest(score, it) }
    }

    private suspend fun readManifest(score: PlatformFile) = manifest(score) ?: throw IOException("${score.name} has no readable manifest")

    private suspend fun writeManifest(score: PlatformFile, manifest: ScoreManifest) {
        (score / CitScore.MANIFEST_FILE).writeString(json.encodeToString(manifest))
    }

    private suspend fun manifest(score: PlatformFile): ScoreManifest? =
        read(score / CitScore.MANIFEST_FILE) { readString() }?.let { decode<ScoreManifest>(it) }

    private inline fun <reified T> decode(text: String): T? = try {
        json.decodeFromString<T>(text)
    } catch (_: IllegalArgumentException) {
        null
    }

    private suspend fun <T> read(file: PlatformFile, block: suspend PlatformFile.() -> T): T? = try {
        if (file.exists()) file.block() else null
    } catch (_: IOException) {
        null
    }

    private fun PlatformFile.isScore() = CitScore.isScoreDirectoryName(name)
}
