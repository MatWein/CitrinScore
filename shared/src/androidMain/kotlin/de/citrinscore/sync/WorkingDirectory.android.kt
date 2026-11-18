package de.citrinscore.sync

import android.os.Build
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile
import java.io.File

internal actual val foldersHaveWorkingDirectory = false

internal actual fun PlatformFile.workingDirectory(): File = when (val file = androidFile) {
    is AndroidFile.FileWrapper -> file.file
    is AndroidFile.UriWrapper -> throw SyncException(SyncFailure.LibraryLocation, "${file.uri} is not in the app storage")
}

internal actual fun deviceName(): String = Build.MODEL
