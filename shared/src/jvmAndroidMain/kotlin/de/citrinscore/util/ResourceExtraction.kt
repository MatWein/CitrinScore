package de.citrinscore.util

import de.citrinscore.resources.Res
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File
import java.util.zip.CRC32

private const val COMPLETE_MARKER = ".complete"
private const val HEX_RADIX = 16
private val logger = LoggerFactory.getLogger("de.citrinscore.util.ResourceExtraction")

internal suspend fun extractResource(resource: String, directory: String, unpack: (ByteArray, File) -> Unit): File {
    val content = Res.readBytes(resource)
    return withContext(Dispatchers.IO) {
        val parent = File(FileKit.filesDir.path, directory)
        val version = CRC32().apply { update(content) }.value.toString(HEX_RADIX)
        val target = File(parent, version)
        if (!File(target, COMPLETE_MARKER).exists()) {
            logger.info("Extracting {} to {}", resource, target)
            parent.listFiles()?.forEach { it.deleteRecursively() }
            target.mkdirs()
            unpack(content, target)
            File(target, COMPLETE_MARKER).createNewFile()
        }
        target
    }
}
