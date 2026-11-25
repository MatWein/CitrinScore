package de.citrinscore.library

import android.content.Intent
import android.provider.DocumentsContract
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context

actual val PlatformFile.canShowInFileManager: Boolean
    get() = androidFile is AndroidFile.UriWrapper

actual suspend fun PlatformFile.showInFileManager() {
    val uri = (androidFile as? AndroidFile.UriWrapper)?.uri ?: error("$this is no shared document")
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, DocumentsContract.Document.MIME_TYPE_DIR)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
    FileKit.context.startActivity(intent)
}
