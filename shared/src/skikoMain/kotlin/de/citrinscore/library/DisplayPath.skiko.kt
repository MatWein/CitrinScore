package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path

actual val PlatformFile.displayPath: String?
    get() = path
