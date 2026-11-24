package de.citrinscore.library

import de.citrinscore.score.Attachment
import de.citrinscore.score.AttachmentKind
import de.citrinscore.score.CitScore
import de.citrinscore.score.PartDisplay
import de.citrinscore.score.PlaybackPreferences
import de.citrinscore.score.Recognition
import de.citrinscore.score.RecognitionLayout
import de.citrinscore.score.RecognizedMusic
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.Normalization
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScorePage
import de.citrinscore.score.ScoreDeviceState
import de.citrinscore.score.ScrollMode
import de.citrinscore.score.deviceState
import de.citrinscore.score.shared
import de.citrinscore.score.withDeviceState
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScorePart
import de.citrinscore.score.ScoreThumbnail
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.isReflowable
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.serialization.AppJson
import de.citrinscore.util.attempt
import de.citrinscore.util.zip
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.copyRecursively
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
private val PAGE_DISPLAYS = listOf(PartDisplay.Original, PartDisplay.Normalized)

class LibraryRepository(
    private val json: Json = AppJson,
    private val clock: Clock = Clock.System,
) {

    private val manifestUpdates = Mutex()

    suspend fun content(folder: PlatformFile): FolderContent = withContext(Dispatchers.IO) {
        val directories = folder.list().filter { !it.name.startsWith(".") && it.isDirectory() }
        val (scoreDirectories, subfolders) = directories.partition { it.isScore() }
        val folders = subfolders.map(LibraryItem::Folder)
        val scores = scoreDirectories.map { directory -> manifest(directory).let { LibraryItem.Score(directory, it?.lastActivity, it?.metadata) } }
        FolderContent(folders + scores)
    }

    /** Everything below [folder], in all subfolders. */
    suspend fun entries(folder: PlatformFile, path: List<PlatformFile> = emptyList()): List<SearchEntry> =
        content(folder).items.flatMap { item ->
            listOf(SearchEntry(item, folder, path)) +
                if (item is LibraryItem.Folder) entries(item.file, path + item.file) else emptyList()
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

    /**
     * Moves or copies [items] from the folder [source] into the folder [target]. Items whose name is already taken in
     * the target get a numbered name, and renamed scores get the matching title. Returns the items that failed.
     */
    suspend fun transfer(items: List<PlatformFile>, source: PlatformFile, target: PlatformFile, mode: TransferMode): List<PlatformFile> =
        withContext(Dispatchers.IO) {
            logger.info("{} {} from {} to {}", mode, items.map { it.name }, source.path, target.path)
            val taken = target.list().mapTo(ArrayList()) { it.name }
            items.filterNot { item ->
                if (target.isInside(item)) return@filterNot false
                if (mode == TransferMode.Move && target.path == source.path) return@filterNot true
                val name = availableName(item.name, taken)
                taken += name
                attempt { transferItem(item, source, target, name, mode) } != null
            }
        }

    private suspend fun transferItem(item: PlatformFile, source: PlatformFile, target: PlatformFile, name: String, mode: TransferMode) {
        val destination = when (mode) {
            TransferMode.Copy -> copyItem(item, target / name)
            TransferMode.Move -> attempt { item.moveDirectory(source, target, name) } ?: copyItem(item, target / name).also {
                logger.info("Moving {} directly failed, copied it instead", item.path)
                delete(item)
            }
        }
        if (name != item.name && CitScore.isScoreDirectoryName(name) && destination.isScore()) {
            attempt { updateManifest(destination) { it.copy(metadata = it.metadata.copy(title = CitScore.title(name))) } }
        }
    }

    private fun PlatformFile.isInside(folder: PlatformFile) = path == folder.path || listOf('/', '\\').any { path.startsWith(folder.path + it) }

    private suspend fun copyItem(item: PlatformFile, destination: PlatformFile): PlatformFile = try {
        item.copyRecursively(destination)
        destination
    } catch (e: Exception) {
        attempt { destination.delete(mustExist = false, recursively = true) }
        throw e
    }

    suspend fun open(score: PlatformFile): ScoreManifest {
        logger.info("Opening score {}", score.path)
        return updateManifest(score) { manifest -> manifest.copy(lastOpenedAt = clock.now()) }
    }

    suspend fun load(score: PlatformFile): ScoreManifest = readManifest(score)

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
                    edit.source?.let { source -> editContent(score, part, source) } ?: part
                }.let { part -> editRecognition(PartRef(score, part.directory).root, part, edit.recognition) }.copy(name = edit.name)
                is PartEdit.Add -> createPart(score, edit.part)
            }
        }
        writeThumbnail(score, parts.first())
        val kept = parts.map { it.directory }.toSet()
        val removed = previous.parts.filterNot { it.directory in kept }
        updateManifest(score) { manifest ->
            manifest.copy(metadata = metadata, parts = parts, currentPart = manifest.currentPart?.takeIf { it in kept })
        }
        removed.forEach { deletePart(score, it) }
        return if (metadata.title == previous.metadata.title) score else score.renameDirectory(folder, CitScore.directoryName(metadata.title))
    }

    suspend fun selectPart(part: PartRef) {
        logger.info("Selecting part {}", part)
        updateManifest(part.score) { it.copy(currentPart = part.directory) }
    }

    suspend fun saveScrollMode(score: PlatformFile, mode: ScrollMode) {
        logger.debug("Saving scroll mode {} of {}", mode, score.path)
        updateManifest(score) { it.copy(scrollMode = mode) }
    }

    suspend fun annotations(display: DisplayRef): ScoreAnnotations =
        read(display.annotations) { readString() }?.let { decode(ScoreAnnotations.serializer(), it) } ?: ScoreAnnotations()

    suspend fun saveAnnotations(display: DisplayRef, annotations: ScoreAnnotations) {
        logger.debug("Saving annotations of {}", display)
        display.annotations.writeString(json.encodeToString(annotations))
    }

    suspend fun saveView(part: PartRef, display: PartDisplay, view: ScoreViewState) {
        logger.debug("Saving {} view {} of {}", display, view, part)
        updatePart(part) { it.withView(display, view) }
    }

    suspend fun saveDisplay(part: PartRef, display: PartDisplay) {
        logger.debug("Saving display {} of {}", display, part)
        updatePart(part) { it.copy(display = display) }
    }

    suspend fun savePlayback(part: PartRef, playback: PlaybackPreferences) {
        logger.debug("Saving playback preferences {} of {}", playback, part)
        updatePart(part) { it.copy(playback = playback) }
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

    suspend fun exportScore(score: PlatformFile, target: PlatformFile) {
        logger.info("Exporting score {} to {}", score.path, target.path)
        val deviceState = "${score.name}/${CitScore.DEVICE_STATE_FILE}"
        val entries = withContext(Dispatchers.IO) { files(score, score.name).filterNot { it.first == deviceState } }
        target.write(withContext(Dispatchers.Default) { zip(entries) })
    }

    private suspend fun files(directory: PlatformFile, prefix: String): List<Pair<String, ByteArray>> = directory.list().flatMap { child ->
        val path = "$prefix/${child.name}"
        if (child.isDirectory()) files(child, path) else listOf(path to child.readBytes())
    }

    suspend fun thumbnail(score: PlatformFile): ByteArray? = read(score / CitScore.THUMBNAIL_FILE) { readBytes() }

    private suspend fun createPart(score: PlatformFile, part: NewPart): ScorePart {
        val directory = CitScore.partDirectory(Uuid.random().toString())
        val root = CitScore.resolve(score, directory)
        try {
            root.createDirectories()
            val content = writeContent(root, part.source, previous = null)
            return ScorePart(
                directory,
                part.name,
                content,
                recognition = part.recognition?.let { writeRecognition(root, it) },
            )
        } catch (e: Exception) {
            root.delete(mustExist = false, recursively = true)
            throw e
        }
    }

    private suspend fun writeContent(root: PlatformFile, source: PartSource, previous: ScoreContent?): ScoreContent = when (source) {
        is PartSource.Pages -> writePages(root, source.pages, previous as? ScoreContent.Pages)
        is PartSource.MusicXml -> {
            val name = "${CitScore.MUSICXML_FILE_NAME}.${source.file.extension.lowercase()}"
            (root / name) write source.file
            (previous as? ScoreContent.MusicXml)?.takeIf { it.path != name }?.let { CitScore.resolve(root, it.path).delete(mustExist = false) }
            ScoreContent.MusicXml(name)
        }
    }

    private suspend fun writePages(root: PlatformFile, pages: List<NewPage>, previous: ScoreContent.Pages?): ScoreContent.Pages {
        val copied = mutableMapOf<String, String>()
        val written = pages.map { page ->
            val source = when (val origin = page.origin) {
                is PageOrigin.Stored -> origin.path
                is PageOrigin.File -> copied.getOrPut(origin.file.path) { storeFile(root, CitScore.SOURCES_DIRECTORY, origin.file.extension) { it write origin.file } }
            }
            val normalized = page.normalization
            val image = storeFile(root, CitScore.PAGES_DIRECTORY, CitScore.SCAN_EXTENSION) { it.write(normalized.image) }
            ScorePage(source, page.page, page.crop, Normalization(image, normalized.threshold, normalized.erased))
        }
        val referenced = written.flatMap { it.files }.toSet()
        previous?.pages.orEmpty().flatMap { it.files }.filterNot { it in referenced }.distinct()
            .forEach { CitScore.resolve(root, it).delete(mustExist = false) }
        return ScoreContent.Pages(written)
    }

    private val ScorePage.files get() = listOf(source, normalization.image)

    private suspend fun storeFile(root: PlatformFile, directory: String, extension: String, write: suspend (PlatformFile) -> Unit): String {
        val path = "$directory/${Uuid.random()}.${extension.lowercase()}"
        (root / directory).createDirectories()
        write(CitScore.resolve(root, path))
        return path
    }

    private suspend fun editContent(score: PlatformFile, part: ScorePart, source: PartSource): ScorePart {
        val ref = PartRef(score, part.directory)
        val content = writeContent(ref.root, source, part.content)
        if (source is PartSource.Pages) {
            val newIndexByOldIndex = source.pages.withIndex().mapNotNull { (index, page) -> page.previousIndex?.let { it to index } }.toMap()
            PAGE_DISPLAYS.map { DisplayRef(ref, it) }.filter { it.annotations.exists() }.forEach { display ->
                saveAnnotations(display, annotations(display).remapPages(newIndexByOldIndex))
            }
        }
        return part.copy(content = content, view = null)
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
                (root / CitScore.annotationsFile(PartDisplay.MusicXml)).delete(mustExist = false)
            }
            part.copy(recognition = null)
        }
        is RecognitionEdit.Replace -> part.copy(recognition = writeRecognition(root, edit.music))
    }

    private suspend fun deletePart(score: PlatformFile, part: ScorePart) {
        PartRef(score, part.directory).root.delete(mustExist = false, recursively = true)
    }

    private suspend fun writeThumbnail(score: PlatformFile, part: ScorePart) {
        ScoreRenderers.open(PartRef(score, part.directory).root, part.content, PartDisplay.Original).use { renderer ->
            (score / CitScore.THUMBNAIL_FILE) write ScoreThumbnail.render(renderer)
        }
    }

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
        val manifest = read(score / CitScore.MANIFEST_FILE) { readString() }?.let { decode(ScoreManifest.serializer(), it) } ?: return null
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
            is PartSource.Pages -> "${pages.size} pages"
            is PartSource.MusicXml -> "MusicXML ${file.path}"
        }

    private val PartEdit.description
        get() = when (this) {
            is PartEdit.Keep -> "keep $directory" + source?.let { " with ${it.description}" }.orEmpty() + " and recognition ${recognition::class.simpleName}"
            is PartEdit.Add -> "add ${part.source.description}"
        }
}
