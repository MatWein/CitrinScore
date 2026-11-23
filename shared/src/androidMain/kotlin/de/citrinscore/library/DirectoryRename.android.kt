package de.citrinscore.library

import android.provider.DocumentsContract
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.atomicMove
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.name
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

internal actual suspend fun PlatformFile.moveDirectory(parent: PlatformFile, target: PlatformFile, name: String): PlatformFile =
    when (val file = androidFile) {
        is AndroidFile.FileWrapper -> (target / name).also { atomicMove(it) }
        is AndroidFile.UriWrapper -> withContext(Dispatchers.IO) {
            val parentUri = (parent.androidFile as? AndroidFile.UriWrapper)?.uri ?: throw IOException("$parent is no document")
            val targetUri = (target.androidFile as? AndroidFile.UriWrapper)?.uri ?: throw IOException("$target is no document")
            if (name != this@moveDirectory.name) throw IOException("Documents cannot be renamed while moving")
            DocumentsContract.moveDocument(FileKit.context.contentResolver, file.uri, parentUri, targetUri) ?: throw IOException("Could not move ${file.uri}")
            target / name
        }
    }
