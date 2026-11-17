package de.citrinscore.sync

import io.github.vinceglb.filekit.PlatformFile
import java.io.File

internal expect fun PlatformFile.workingDirectory(): File

internal expect fun deviceName(): String
