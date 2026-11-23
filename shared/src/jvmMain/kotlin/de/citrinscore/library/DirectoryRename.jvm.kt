package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.div

internal actual suspend fun PlatformFile.renameDirectory(parent: PlatformFile, name: String): PlatformFile =
    (parent / name).also { atomicMove(it) }

internal actual suspend fun PlatformFile.moveDirectory(parent: PlatformFile, target: PlatformFile, name: String): PlatformFile =
    (target / name).also { atomicMove(it) }
