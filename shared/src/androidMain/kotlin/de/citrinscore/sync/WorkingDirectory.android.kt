package de.citrinscore.sync

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import java.io.File

private const val EXTERNAL_STORAGE_AUTHORITY = "com.android.externalstorage.documents"
private const val PRIMARY_VOLUME = "primary"
private const val VOLUME_SEPARATOR = ':'

internal actual fun PlatformFile.workingDirectory(): File {
    if (!hasLibraryAccess()) throw SyncException(SyncFailure.StorageAccess, "All files access is not granted")
    return when (val file = androidFile) {
        is AndroidFile.FileWrapper -> file.file
        is AndroidFile.UriWrapper -> file.uri.storageDirectory() ?: throw SyncException(SyncFailure.LibraryLocation, "${file.uri} is not on device storage")
    }
}

internal actual fun deviceName(): String = Build.MODEL

private fun Uri.storageDirectory(): File? {
    if (authority != EXTERNAL_STORAGE_AUTHORITY) return null
    val document = if (DocumentsContract.isDocumentUri(FileKit.context, this)) DocumentsContract.getDocumentId(this) else DocumentsContract.getTreeDocumentId(this)
    val volume = document.substringBefore(VOLUME_SEPARATOR)
    val root = if (volume == PRIMARY_VOLUME) Environment.getExternalStorageDirectory() else secondaryVolume(volume) ?: return null
    return File(root, document.substringAfter(VOLUME_SEPARATOR, ""))
}

private fun secondaryVolume(uuid: String): File? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
    val storage = FileKit.context.getSystemService(Context.STORAGE_SERVICE) as StorageManager
    return storage.storageVolumes.firstOrNull { it.uuid.equals(uuid, ignoreCase = true) }?.directory
}
