package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.io.IOException

actual val PlatformFile.canShowInFileManager: Boolean
    get() = true

actual suspend fun PlatformFile.showInFileManager() {
    val folder = file
    withContext(Dispatchers.IO) {
        if (!folder.isDirectory) throw IOException("$folder is no folder")
        if (isLinux || !Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
            ProcessBuilder(openCommand(folder)).inheritIO().start()
        } else {
            Desktop.getDesktop().open(folder)
        }
    }
}

private val osName = System.getProperty("os.name").orEmpty().lowercase()

private val isLinux = "linux" in osName || "bsd" in osName

private fun openCommand(folder: File) = when {
    "windows" in osName -> listOf("explorer.exe", folder.absolutePath)
    "mac" in osName -> listOf("open", folder.absolutePath)
    else -> listOf("xdg-open", folder.absolutePath)
}
