package de.citrinscore.files

import android.os.ParcelFileDescriptor
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import java.io.IOException

internal fun PlatformFile.openReadOnlyDescriptor(): ParcelFileDescriptor = when (val file = androidFile) {
    is AndroidFile.FileWrapper -> ParcelFileDescriptor.open(file.file, ParcelFileDescriptor.MODE_READ_ONLY)
    is AndroidFile.UriWrapper -> FileKit.context.contentResolver.openFileDescriptor(file.uri, "r")
        ?: throw IOException("Could not open ${file.uri}")
}
