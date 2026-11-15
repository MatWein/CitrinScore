package de.citrinscore.archive

import io.github.vinceglb.filekit.PlatformFile

actual suspend fun PlatformFile.writeZipArchive(entries: List<ArchiveEntry>): Unit =
    TODO("ZIP archives are not supported on iOS yet")
