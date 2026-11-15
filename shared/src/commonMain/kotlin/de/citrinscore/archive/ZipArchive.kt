package de.citrinscore.archive

import io.github.vinceglb.filekit.PlatformFile

class ArchiveEntry(val name: String, val content: suspend () -> ByteArray)

expect suspend fun PlatformFile.writeZipArchive(entries: List<ArchiveEntry>)
