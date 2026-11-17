package de.citrinscore.score.musicxml

import de.citrinscore.resources.Res
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.zip.CRC32
import java.util.zip.ZipInputStream

private const val RESOURCES_ARCHIVE = "files/verovio.zip"
private const val RESOURCES_DIRECTORY = "verovio"
private const val COMPLETE_MARKER = ".complete"
private const val HEX_RADIX = 16
private const val TEXT_FONT_FAMILY = "Leipzig"
private const val TEXT_FONT_FILE = "$TEXT_FONT_FAMILY.ttf"

private val extraction = Mutex()
private var textFontRegistered = false

internal suspend fun verovioResourcePath(): String = extraction.withLock {
    val archive = Res.readBytes(RESOURCES_ARCHIVE)
    withContext(Dispatchers.IO) {
        extract(archive).also { path ->
            if (!textFontRegistered) {
                registerTextFont(TEXT_FONT_FAMILY, File(path, TEXT_FONT_FILE))
                textFontRegistered = true
            }
        }
    }
}

private fun extract(archive: ByteArray): String {
    val parent = File(FileKit.filesDir.path, RESOURCES_DIRECTORY)
    val version = CRC32().apply { update(archive) }.value.toString(HEX_RADIX)
    val target = File(parent, version)
    if (!File(target, COMPLETE_MARKER).exists()) {
        parent.listFiles()?.forEach { it.deleteRecursively() }
        ZipInputStream(archive.inputStream()).use { zip ->
            generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.forEach { entry ->
                val file = File(target, entry.name)
                require(file.canonicalPath.startsWith(target.canonicalPath + File.separator)) { "Invalid entry ${entry.name}" }
                file.parentFile.mkdirs()
                file.outputStream().use { zip.copyTo(it) }
            }
        }
        File(target, COMPLETE_MARKER).createNewFile()
    }
    return target.absolutePath
}
