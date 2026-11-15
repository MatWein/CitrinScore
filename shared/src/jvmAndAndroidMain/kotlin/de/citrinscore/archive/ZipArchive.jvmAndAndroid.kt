package de.citrinscore.archive

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.asOutputStream
import kotlinx.io.buffered
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual suspend fun PlatformFile.writeZipArchive(entries: List<ArchiveEntry>) = withContext(Dispatchers.IO) {
    ZipOutputStream(sink().buffered().asOutputStream()).use { zip ->
        entries.forEach { entry ->
            zip.putNextEntry(ZipEntry(entry.name))
            zip.write(entry.content())
            zip.closeEntry()
        }
    }
}
