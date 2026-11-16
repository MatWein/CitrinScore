package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.div

internal actual suspend fun PlatformFile.renameDirectory(parent: PlatformFile, name: String): PlatformFile =
    (parent / name).also { atomicMove(it) }
