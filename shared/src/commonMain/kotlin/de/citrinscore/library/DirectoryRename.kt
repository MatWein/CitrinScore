package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile

internal expect suspend fun PlatformFile.renameDirectory(parent: PlatformFile, name: String): PlatformFile
