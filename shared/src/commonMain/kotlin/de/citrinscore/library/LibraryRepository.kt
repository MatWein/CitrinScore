package de.citrinscore.library

import de.citrinscore.score.Attachment
import de.citrinscore.score.AttachmentKind
import de.citrinscore.score.CitScore
import de.citrinscore.score.ImagePage
import de.citrinscore.score.PageCrop
import de.citrinscore.score.PdfPage
import de.citrinscore.score.PlaybackPreferences
import de.citrinscore.score.Recognition
import de.citrinscore.score.RecognitionLayout
import de.citrinscore.score.RecognizedMusic
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreDeviceState
import de.citrinscore.score.deviceState
import de.citrinscore.score.shared
import de.citrinscore.score.withDeviceState
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreManifestSerializer
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScorePart
import de.citrinscore.score.ScoreThumbnail
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.isReflowable
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.contentCrop
import de.citrinscore.score.render.openImageSource
import de.citrinscore.score.render.systemBreaks
import de.citrinscore.serialization.AppJson
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.extension
import io.github.vinceglb.filekit.isDirectory
import io.github.vinceglb.filekit.list
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
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
import org.slf4j.LoggerFactory
import kotlin.time.Clock
import kotlin.uuid.Uuid

private val logger = LoggerFactory.getLogger(LibraryRepository::class.java)

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
        logger.info("Creating folder {} in {}", name, parent.path)
        (parent / name).createDirectories()
    }

    suspend fun delete(item: PlatformFile) = withContext(Dispatchers.IO) {
        logger.info("Deleting {}", item.path)
        item.delete(mustExist = false, recursively = true)
        if (item.exists()) throw IOException("${item.name} could not be deleted")
    }

    suspend fun open(score: PlatformFile): ScoreManifest {
        logger.info("Opening score {}", score.path)
        return updateManifest(score) { manifest -> manifest.withCrops(score).copy(lastOpenedAt = clock.now()) }
    }

    suspend fun load(score: PlatformFile): ScoreManifest = readManifest(score).withCrops(score)

    suspend fun importScore(folder: PlatformFile, metadata: ScoreMetadata, parts: List<NewPart>) {
        val score = folder / CitScore.directoryName(metadata.title)
        logger.info("Importing score {} with parts {}", score.path, parts.map { it.source.description })
        try {
            score.createDirectories()
            val created = parts.map { createPart(score, it) }
            writeThumbnail(score, created.first())
            writeManifest(score, ScoreManifest(metadata, created, createdAt = clock.now()))
        } catch (e: Exception) {
            logger.warn("Importing score {} failed, removing it", score.path)
            score.delete(mustExist = false, recursively = true)
            throw e
        }
    }

    suspend fun updateScore(folder: PlatformFile, score: PlatformFile, metadata: ScoreMetadata, edits: List<PartEdit>): PlatformFile {
        logger.info("Updating score {} with parts {}", score.path, edits.map { it.description })
        val previous = load(score)
        val parts = edits.map { edit ->
            when (edit) {
                is PartEdit.Keep -> previous.part(edit.directory).let { part ->
                    edit.pages?.let { pages -> editPages(score, part, pages) } ?: part
                }.let { part -> editRecognition(PartRef(score, part.directory).root, part, edit.recognition) }.copy(name = edit.name)
                is PartEdit.Add -> createPart(score, edit.part)
            }
        }
        writeThumbnail(score, parts.first())
        val kept = parts.map { it.directory }.toSet()
        val removed = previous.parts.filterNot { it.directory in kept }
        updateManifest(score) { manifest ->
            manifest.copy(metadata = metadata, parts = parts, currentPart = manifest.currentPart?.takeIf { it in kept })
                .withAttachmentsMovedOutOf(score, removed.map { it.directory })
        }
        removed.forEach { deletePart(score, it) }
        return if (metadata.title == previous.metadata.title) score else score.renameDirectory(folder, CitScore.directoryName(metadata.title))
    }

    suspend fun selectPart(part: PartRef) {
        logger.info("Selecting part {}", part)
        updateManifest(part.score) { it.copy(currentPart = part.directory) }
    }

    suspend fun annotations(part: PartRef): ScoreAnnotations =
        read(part.root / CitScore.ANNOTATIONS_FILE) { readString() }?.let { decode(ScoreAnnotations.serializer(), it) } ?: ScoreAnnotations()

    suspend fun saveAnnotations(part: PartRef, annotations: ScoreAnnotations) {
        logger.debug("Saving annotations of {}", part)
        (part.root / CitScore.ANNOTATIONS_FILE).writeString(json.encodeToString(annotations))
    }

    suspend fun saveView(part: PartRef, view: ScoreViewState) {
        logger.debug("Saving view {} of {}", view, part)
        updatePart(part) { it.copy(view = view) }
    }

    suspend fun savePlayback(part: PartRef, playback: PlaybackPreferences) {
        logger.debug("Saving playback preferences {} of {}", playback, part)
        updatePart(part) { it.copy(playback = playback) }
    }

    suspend fun saveSystemBreaks(part: PartRef, systemBreaks: List<List<Float>>) {
        logger.debug("Saving staff systems of {} pages of {}", systemBreaks.size, part)
        updatePart(part) { it.copy(systemBreaks = systemBreaks) }
    }

    suspend fun attachments(score: PlatformFile, kind: AttachmentKind): List<Attachment> = manifest(score)?.attachments(kind).orEmpty()

    suspend fun addAttachment(score: PlatformFile, kind: AttachmentKind, source: PlatformFile, name: String): Attachment {
        val suffix = source.extension.takeIf { it.isNotEmpty() }?.let { ".${it.lowercase()}" }.orEmpty()
        val attachment = Attachment(kind.path("${Uuid.random()}$suffix"), name)
        logger.info("Adding {} attachment {} from {} to {}", kind, attachment.path, source.path, score.path)
        (score / kind.directory).createDirectories()
        CitScore.resolve(score, attachment.path) write source
        updateManifest(score) { it.withAttachments(kind, it.attachments(kind) + attachment) }
        return attachment
    }

    suspend fun renameAttachment(score: PlatformFile, kind: AttachmentKind, attachment: Attachment, name: String) {
        logger.info("Renaming attachment {} of {}", attachment.path, score.path)
        updateManifest(score) { manifest ->
            manifest.withAttachments(kind, manifest.attachments(kind).map { if (it.path == attachment.path) it.copy(name = name) else it })
        }
    }

    suspend fun deleteAttachment(score: PlatformFile, kind: AttachmentKind, attachment: Attachment) {
        logger.info("Deleting attachment {} of {}", attachment.path, score.path)
        updateManifest(score) { manifest -> manifest.withAttachments(kind, manifest.attachments(kind).filterNot { it.path == attachment.path }) }
        CitScore.resolve(score, attachment.path).delete(mustExist = false)
    }

    suspend fun exportAttachment(score: PlatformFile, attachment: Attachment, target: PlatformFile) {
        logger.info("Exporting attachment {} of {} to {}", attachment.path, score.path, target.path)
        target write CitScore.resolve(score, attachment.path)
    }

    suspend fun thumbnail(score: PlatformFile): ByteArray? = read(score / CitScore.THUMBNAIL_FILE) { readBytes() }

    private suspend fun createPart(score: PlatformFile, part: NewPart): ScorePart {
        val directory = CitScore.partDirectory(Uuid.random().toString())
        val root = CitScore.resolve(score, directory)
        try {
            root.createDirectories()
            val content = writeContent(root, part.source)
            return ScorePart(
                directory,
                part.name,
                content,
                systemBreaks = content.takeUnless { it.isReflowable }?.let { systemBreaks(root, it) },
                recognition = part.recognition?.let { writeRecognition(root, it) },
            )
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
        is PartSource.MusicXml -> {
            val name = "${CitScore.MUSICXML_FILE_NAME}.${source.file.extension.lowercase()}"
            (root / name) write source.file
            ScoreContent.MusicXml(name)
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
            is ScoreContent.MusicXml -> throw IllegalArgumentException("The pages of a MusicXML part cannot be edited")
        }
        if (content == part.content) return part
        removedImages(part.content, content).forEach { CitScore.resolve(root, it.path).delete(mustExist = false) }
        val newIndexByOldIndex = pages.withIndex().mapNotNull { (index, page) -> (page as? PageEdit.Keep)?.let { it.index to index } }.toMap()
        if ((root / CitScore.ANNOTATIONS_FILE).exists()) saveAnnotations(ref, annotations(ref).remapPages(newIndexByOldIndex))
        return part.copy(content = content, systemBreaks = systemBreaks(root, content), view = null)
    }

    private suspend fun ScoreManifest.withAttachmentsMovedOutOf(score: PlatformFile, directories: List<String>) =
        AttachmentKind.entries.fold(this) { manifest, kind ->
            manifest.withAttachments(
                kind,
                manifest.attachments(kind).map { attachment ->
                    if (directories.any { attachment.path.startsWith("$it/") }) moveAttachment(score, kind, attachment) else attachment
                },
            )
        }

    private suspend fun moveAttachment(score: PlatformFile, kind: AttachmentKind, attachment: Attachment): Attachment {
        val moved = attachment.copy(path = kind.path(attachment.path.substringAfterLast('/')))
        logger.info("Moving attachment {} of {} to {}", attachment.path, score.path, moved.path)
        (score / kind.directory).createDirectories()
        CitScore.resolve(score, attachment.path).atomicMove(CitScore.resolve(score, moved.path))
        return moved
    }

    suspend fun recognition(part: PartRef, recognition: Recognition): RecognizedMusic? {
        val musicXml = read(CitScore.resolve(part.root, recognition.musicXml)) { readString() } ?: return null
        val layout = read(CitScore.resolve(part.root, recognition.layout)) { readString() } ?: return null
        return RecognizedMusic(musicXml, json.decodeFromString(RecognitionLayout.serializer(), layout))
    }

    private suspend fun writeRecognition(root: PlatformFile, music: RecognizedMusic): Recognition {
        val recognition = Recognition()
        CitScore.resolve(root, recognition.musicXml).writeString(music.musicXml)
        CitScore.resolve(root, recognition.layout).writeString(json.encodeToString(RecognitionLayout.serializer(), music.layout))
        return recognition
    }

    private suspend fun editRecognition(root: PlatformFile, part: ScorePart, edit: RecognitionEdit): ScorePart = when (edit) {
        RecognitionEdit.Unchanged -> part
        RecognitionEdit.Remove -> {
            part.recognition?.let { recognition ->
                CitScore.resolve(root, recognition.musicXml).delete(mustExist = false)
                CitScore.resolve(root, recognition.layout).delete(mustExist = false)
            }
            part.copy(recognition = null)
        }
        is RecognitionEdit.Replace -> part.copy(recognition = writeRecognition(root, edit.music))
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
        is ScoreContent.Pdf, is ScoreContent.MusicXml -> this
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
        logger.debug("Writing {} and {} of {}", CitScore.MANIFEST_FILE, CitScore.DEVICE_STATE_FILE, score.path)
        (score / CitScore.MANIFEST_FILE).writeString(json.encodeToString(manifest.shared))
        (score / CitScore.DEVICE_STATE_FILE).writeString(json.encodeToString(manifest.deviceState))
    }

    private suspend fun manifest(score: PlatformFile): ScoreManifest? {
        val manifest = read(score / CitScore.MANIFEST_FILE) { readString() }?.let { decode(ScoreManifestSerializer, it) } ?: return null
        val state = read(score / CitScore.DEVICE_STATE_FILE) { readString() }?.let { decode(ScoreDeviceState.serializer(), it) }
        return state?.let(manifest::withDeviceState) ?: manifest
    }

    private fun <T> decode(deserializer: DeserializationStrategy<T>, text: String): T? = try {
        json.decodeFromString(deserializer, text)
    } catch (e: IllegalArgumentException) {
        logger.warn("Invalid {}", deserializer.descriptor.serialName, e)
        null
    }

    private suspend fun <T> read(file: PlatformFile, block: suspend PlatformFile.() -> T): T? = try {
        if (file.exists()) file.block() else null
    } catch (e: IOException) {
        logger.warn("Reading {} failed", file, e)
        null
    }

    private fun PlatformFile.isScore() = CitScore.isScoreDirectoryName(name)

    private val PartSource.description
        get() = when (this) {
            is PartSource.Images -> "${files.size} images"
            is PartSource.Scans -> "${images.size} scans"
            is PartSource.Pdf -> "PDF ${file.path}"
            is PartSource.MusicXml -> "MusicXML ${file.path}"
        }

    private val PartEdit.description
        get() = when (this) {
            is PartEdit.Keep -> "keep $directory" + pages?.let { " with ${it.size} pages" }.orEmpty() + " and recognition ${recognition::class.simpleName}"
            is PartEdit.Add -> "add ${part.source.description}"
        }
}
