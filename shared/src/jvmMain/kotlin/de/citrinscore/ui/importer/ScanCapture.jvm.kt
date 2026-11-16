package de.citrinscore.ui.importer

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher

actual val scanUsesCamera = false

@Composable
actual fun isScanAvailable() = true

@Composable
actual fun rememberScanLauncher(onCaptured: (List<PlatformFile>) -> Unit): () -> Unit {
    val launcher = rememberFilePickerLauncher(FileKitType.File(ImportType.Scan.extensions), FileKitMode.Multiple()) { files ->
        files?.let(onCaptured)
    }
    return launcher::launch
}
