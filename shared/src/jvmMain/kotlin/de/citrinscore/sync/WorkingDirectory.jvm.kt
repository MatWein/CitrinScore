package de.citrinscore.sync

import io.github.vinceglb.filekit.PlatformFile
import java.net.InetAddress

internal actual val foldersHaveWorkingDirectory = true

internal actual fun PlatformFile.workingDirectory() = file

internal actual fun deviceName(): String = InetAddress.getLocalHost().hostName
