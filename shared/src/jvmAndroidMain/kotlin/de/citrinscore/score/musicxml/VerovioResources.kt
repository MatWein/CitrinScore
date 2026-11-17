package de.citrinscore.score.musicxml

import de.citrinscore.util.extractResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.ZipInputStream

private const val RESOURCES_ARCHIVE = "files/verovio.zip"
private const val RESOURCES_DIRECTORY = "verovio"
private const val TEXT_FONT_FAMILY = "Leipzig"
private const val TEXT_FONT_FILE = "$TEXT_FONT_FAMILY.ttf"
private const val GLYPH_BOUNDS_FILE = "$TEXT_FONT_FAMILY.xml"

private val extraction = Mutex()
private var textFontRegistered = false
private var glyphBounds: GlyphBounds? = null

internal suspend fun verovioResourcePath(): String = extraction.withLock {
    val path = extractResource(RESOURCES_ARCHIVE, RESOURCES_DIRECTORY, ::unzip)
    if (!textFontRegistered) {
        withContext(Dispatchers.IO) { registerTextFont(TEXT_FONT_FAMILY, File(path, TEXT_FONT_FILE)) }
        textFontRegistered = true
    }
    path.absolutePath
}

internal suspend fun verovioGlyphBounds(): GlyphBounds {
    val path = verovioResourcePath()
    return extraction.withLock {
        glyphBounds ?: withContext(Dispatchers.IO) { GlyphBounds(File(path, GLYPH_BOUNDS_FILE)) }.also { glyphBounds = it }
    }
}

private fun unzip(archive: ByteArray, target: File) {
    ZipInputStream(archive.inputStream()).use { zip ->
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
            val file = File(target, entry.name)
            require(file.canonicalPath.startsWith(target.canonicalPath + File.separator)) { "Invalid entry ${entry.name}" }
            file.parentFile.mkdirs()
            file.outputStream().use { zip.copyTo(it) }
        }
    }
}
