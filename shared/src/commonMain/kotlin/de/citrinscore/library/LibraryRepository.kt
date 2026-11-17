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
import de.citrinscore.score.ScoreManifestSerializer
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScorePart
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
import kotlinx.serialization.DeserializationStrategy
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
        manifest.withCrops(score).copy(lastOpenedAt = clock.now())
    }

    suspend fun load(score: PlatformFile): ScoreManifest = readManifest(score).withCrops(score)

    suspend fun importScore(folder: PlatformFile, metadata: ScoreMetadata, parts: List<NewPart>) {
        val score = folder / CitScore.directoryName(metadata.title)
        try {
            score.createDirectories()
            val created = parts.map { createPart(score, it) }
            writeThumbnail(score, created.first())
            writeManifest(score, ScoreManifest(metadata, created, createdAt = clock.now()))
        } catch (e: Exception) {
            score.delete(mustExist = false, recursively = true)
            throw e
        }
    }

    suspend fun updateScore(folder: PlatformFile, score: PlatformFile, metadata: ScoreMetadata, edits: List<PartEdit>): PlatformFile {
        val previous = load(score)
        val parts = edits.map { edit ->
            when (edit) {
                is PartEdit.Keep -> previous.part(edit.directory).let { part ->
                    edit.pages?.let { pages -> editPages(score, part, pages) } ?: part
                }.copy(name = edit.name)
                is PartEdit.Add -> createPart(score, edit.part)
            }
        }
        writeThumbnail(score, parts.first())
        val kept = parts.map { it.directory }.toSet()
        updateManifest(score) { manifest ->
            manifest.copy(metadata = metadata, parts = parts, currentPart = manifest.currentPart?.takeIf { it in kept })
        }
        previous.parts.filterNot { it.directory in kept }.forEach { deletePart(score, it) }
        return if (metadata.title == previous.metadata.title) score else score.renameDirectory(folder, CitScore.directoryName(metadata.title))
    }

    suspend fun selectPart(part: PartRef) {
        updateManifest(part.score) { it.copy(currentPart = part.directory) }
    }

    suspend fun annotations(part: PartRef): ScoreAnnotations =
        read(part.root / CitScore.ANNOTATIONS_FILE) { readString() }?.let { decode(ScoreAnnotations.serializer(), it) } ?: ScoreAnnotations()

    suspend fun saveAnnotations(part: PartRef, annotations: ScoreAnnotations) {
        (part.root / CitScore.ANNOTATIONS_FILE).writeString(json.encodeToString(annotations))
    }

    suspend fun saveView(part: PartRef, view: ScoreViewState) {
        updatePart(part) { it.copy(view = view) }
    }

    suspend fun saveSystemBreaks(part: PartRef, systemBreaks: List<List<Float>>) {
        updatePart(part) { it.copy(systemBreaks = systemBreaks) }
    }

    suspend fun attachments(part: PartRef, kind: AttachmentKind): List<Attachment> =
        manifest(part.score)?.parts?.firstOrNull { it.directory == part.directory }?.attachments(kind).orEmpty()

    suspend fun addAttachment(part: PartRef, kind: AttachmentKind, source: PlatformFile, name: String): Attachment {
        val suffix = source.extension.takeIf { it.isNotEmpty() }?.let { ".${it.lowercase()}" }.orEmpty()
        val attachment = Attachment("${kind.directory}/${Uuid.random()}$suffix", name)
        (part.root / kind.directory).createDirectories()
        CitScore.resolve(part.root, attachment.path) write source
        updatePart(part) { it.withAttachments(kind, it.attachments(kind) + attachment) }
        return attachment
    }

    suspend fun renameAttachment(part: PartRef, kind: AttachmentKind, attachment: Attachment, name: String) {
        updatePart(part) { scorePart ->
            scorePart.withAttachments(kind, scorePart.attachments(kind).map { if (it.path == attachment.path) it.copy(name = name) else it })
        }
    }

    suspend fun deleteAttachment(part: PartRef, kind: AttachmentKind, attachment: Attachment) {
        updatePart(part) { scorePart -> scorePart.withAttachments(kind, scorePart.attachments(kind).filterNot { it.path == attachment.path }) }
        CitScore.resolve(part.root, attachment.path).delete(mustExist = false)
    }

    suspend fun exportAttachment(part: PartRef, attachment: Attachment, target: PlatformFile) {
        target write CitScore.resolve(part.root, attachment.path)
    }

    suspend fun thumbnail(score: PlatformFile): ByteArray? = read(score / CitScore.THUMBNAIL_FILE) { readBytes() }

    private suspend fun createPart(score: PlatformFile, part: NewPart): ScorePart {
        val directory = CitScore.partDirectory(Uuid.random().toString())
        val root = CitScore.resolve(score, directory)
        try {
            root.createDirectories()
            val content = writeContent(root, part.source)
            return ScorePart(directory, part.name, content, systemBreaks = systemBreaks(root, content))
        } catch (e: Exception) {
            root.delete(mustExist = false, recursively = true)
            throw e
        }
    }

    private suspend fun writeContent(root: PlatformFile, source: PartSource): ScoreContent = when (source) {
        is PartSource.Images -> writePages(root, source.files.map { it.extension }, source.crops) { index, target -> target write source.files[index] }
        is PartSource.Scans -> writePages(root, source.images.map { CitScore.SCAN_EXTENSION }, source.crops) { index, target ->
            target.write(source.images[index])
        }
        is PartSource.Pdf -> {
            (root / CitScore.PDF_FILE) write source.file
            ScoreContent.Pdf(CitScore.PDF_FILE, source.crops.mapIndexed(::PdfPage))
        }
    }

    private suspend fun writePages(
        root: PlatformFile,
        extensions: List<String>,
        crops: List<PageCrop>,
        writePage: suspend (index: Int, target: PlatformFile) -> Unit,
    ): ScoreContent {
        val pages = root / CitScore.PAGES_DIRECTORY
        pages.createDirectories()
        val names = extensions.mapIndexed { index, extension -> CitScore.pageFileName(index, extension) }
        names.forEachIndexed { index, name -> writePage(index, pages / name) }
        return ScoreContent.Images(names.zip(crops) { name, crop -> ImagePage("${CitScore.PAGES_DIRECTORY}/$name", crop) })
    }

    private suspend fun editPages(score: PlatformFile, part: ScorePart, pages: List<PageEdit>): ScorePart {
        val ref = PartRef(score, part.directory)
        val root = ref.root
        val content = when (val current = part.content) {
            is ScoreContent.Images -> ScoreContent.Images(
                pages.map { page ->
                    when (page) {
                        is PageEdit.Keep -> current.pages[page.index]
                        is PageEdit.Add -> addPage(root, page)
                    }
                },
            )
            is ScoreContent.Pdf -> ScoreContent.Pdf(
                current.path,
                pages.map { page ->
                    require(page is PageEdit.Keep) { "Pages cannot be added to a PDF part" }
                    current.pages[page.index]
                },
            )
        }
        if (content == part.content) return part
        removedImages(part.content, content).forEach { CitScore.resolve(root, it.path).delete(mustExist = false) }
        val newIndexByOldIndex = pages.withIndex().mapNotNull { (index, page) -> (page as? PageEdit.Keep)?.let { it.index to index } }.toMap()
        if ((root / CitScore.ANNOTATIONS_FILE).exists()) saveAnnotations(ref, annotations(ref).remapPages(newIndexByOldIndex))
        return part.copy(content = content, systemBreaks = systemBreaks(root, content), view = null)
    }

    private suspend fun deletePart(score: PlatformFile, part: ScorePart) {
        val root = PartRef(score, part.directory).root
        if (part.directory.isEmpty()) {
            CitScore.ROOT_PART_ENTRIES.forEach { (root / it).delete(mustExist = false, recursively = true) }
        } else {
            root.delete(mustExist = false, recursively = true)
        }
    }

    private suspend fun systemBreaks(root: PlatformFile, content: ScoreContent): List<List<Float>> =
        ScoreRenderers.open(root, content).use { renderer -> renderer.pageSizes.indices.map { renderer.systemBreaks(it) } }

    private suspend fun writeThumbnail(score: PlatformFile, part: ScorePart) {
        ScoreRenderers.open(PartRef(score, part.directory).root, part.content).use { renderer ->
            (score / CitScore.THUMBNAIL_FILE) write ScoreThumbnail.render(renderer)
        }
    }

    private suspend fun addPage(root: PlatformFile, page: PageEdit.Add): ImagePage {
        val path = "${CitScore.PAGES_DIRECTORY}/${Uuid.random()}.${page.image.extension.lowercase()}"
        (root / CitScore.PAGES_DIRECTORY).createDirectories()
        CitScore.resolve(root, path) write page.image
        return ImagePage(path, page.crop)
    }

    private fun removedImages(previous: ScoreContent, current: ScoreContent): List<ImagePage> {
        if (previous !is ScoreContent.Images || current !is ScoreContent.Images) return emptyList()
        val kept = current.pages.map { it.path }.toSet()
        return previous.pages.filterNot { it.path in kept }
    }

    private suspend fun ScoreManifest.withCrops(score: PlatformFile) =
        copy(parts = parts.map { part -> part.copy(content = part.content.withCrops(PartRef(score, part.directory).root)) })

    private suspend fun ScoreContent.withCrops(root: PlatformFile): ScoreContent = when (this) {
        is ScoreContent.Images -> copy(pages = pages.map { page -> page.crop?.let { page } ?: page.copy(crop = imageCrop(CitScore.resolve(root, page.path))) })
        is ScoreContent.Pdf -> this
    }

    private suspend fun imageCrop(image: PlatformFile) = openImageSource(image).use { it.contentCrop(0) }

    private suspend fun updatePart(part: PartRef, transform: (ScorePart) -> ScorePart) {
        updateManifest(part.score) { it.updatePart(part.directory, transform) }
    }

    private suspend fun updateManifest(score: PlatformFile, transform: suspend (ScoreManifest) -> ScoreManifest) = manifestUpdates.withLock {
        transform(readManifest(score)).also { writeManifest(score, it) }
    }

    private suspend fun readManifest(score: PlatformFile) = manifest(score) ?: throw IOException("${score.name} has no readable manifest")

    private suspend fun writeManifest(score: PlatformFile, manifest: ScoreManifest) {
        (score / CitScore.MANIFEST_FILE).writeString(json.encodeToString(manifest))
    }

    private suspend fun manifest(score: PlatformFile): ScoreManifest? =
        read(score / CitScore.MANIFEST_FILE) { readString() }?.let { decode(ScoreManifestSerializer, it) }

    private fun <T> decode(deserializer: DeserializationStrategy<T>, text: String): T? = try {
        json.decodeFromString(deserializer, text)
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
