package de.citrinscore.library

import android.provider.DocumentsContract
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.div
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

internal actual suspend fun PlatformFile.renameDirectory(parent: PlatformFile, name: String): PlatformFile =
    when (val file = androidFile) {
        is AndroidFile.FileWrapper -> (parent / name).also { atomicMove(it) }
        is AndroidFile.UriWrapper -> withContext(Dispatchers.IO) {
            DocumentsContract.renameDocument(FileKit.context.contentResolver, file.uri, name) ?: throw IOException("Could not rename ${file.uri}")
            parent / name
        }
    }
