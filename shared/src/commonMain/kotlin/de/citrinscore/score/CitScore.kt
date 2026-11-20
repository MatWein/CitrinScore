package de.citrinscore.score

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div

object CitScore {
    const val EXTENSION = "citscore"
    const val MANIFEST_FILE = "manifest.json"
    const val DEVICE_STATE_FILE = "device.json"
    const val THUMBNAIL_FILE = "thumbnail.jpg"
    const val RECOGNITION_MUSICXML_FILE = "recognition.musicxml"
    const val RECOGNITION_LAYOUT_FILE = "recognition.json"
    const val PAGES_DIRECTORY = "pages"
    const val SOURCES_DIRECTORY = "sources"
    const val PARTS_DIRECTORY = "parts"
    const val MUSICXML_FILE_NAME = "score"
    const val COMPRESSED_MUSICXML_EXTENSION = "mxl"
    const val MUSICXML_EXTENSION = "musicxml"
    const val SCAN_EXTENSION = "png"
    const val RECORDING_EXTENSION = "wav"
    val AUDIO_EXTENSIONS = listOf(RECORDING_EXTENSION, "mp3")
    const val THUMBNAIL_WIDTH = 600
    const val PAGE_ASPECT_RATIO = 210f / 297f

    private const val SUFFIX = ".$EXTENSION"

    fun directoryName(title: String) = "$title$SUFFIX"

    fun isScoreDirectoryName(name: String) = name.endsWith(SUFFIX, ignoreCase = true)

    fun title(directoryName: String) = directoryName.dropLast(SUFFIX.length)

    fun resolve(score: PlatformFile, relativePath: String) = relativePath.split('/').filter { it.isNotEmpty() }.fold(score) { directory, name -> directory / name }

    fun resolvePath(directory: String, relativePath: String) = listOf(directory, relativePath).filter { it.isNotEmpty() }.joinToString("/")

    fun partDirectory(id: String) = "$PARTS_DIRECTORY/$id"

    fun annotationsFile(display: PartDisplay) = "annotations-${display.name.lowercase()}.json"
}
