package de.citrinscore.library

import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile

actual val PlatformFile.displayPath: String?
    get() = when (val file = androidFile) {
        is AndroidFile.FileWrapper -> file.file.absolutePath
        is AndroidFile.UriWrapper -> null
    }
