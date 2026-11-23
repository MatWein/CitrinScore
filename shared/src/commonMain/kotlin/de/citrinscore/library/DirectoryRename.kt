package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile

internal expect suspend fun PlatformFile.renameDirectory(parent: PlatformFile, name: String): PlatformFile

/** Moves this directory from [parent] into [target] as [name] without copying, or throws if the platform cannot. */
internal expect suspend fun PlatformFile.moveDirectory(parent: PlatformFile, target: PlatformFile, name: String): PlatformFile
