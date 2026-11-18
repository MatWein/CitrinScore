package de.citrinscore.sync

import io.github.vinceglb.filekit.PlatformFile
import java.io.File

internal expect val foldersHaveWorkingDirectory: Boolean

internal expect fun PlatformFile.workingDirectory(): File

internal expect fun deviceName(): String
