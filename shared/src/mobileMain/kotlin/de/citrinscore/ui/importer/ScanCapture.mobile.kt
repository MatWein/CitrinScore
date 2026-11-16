package de.citrinscore.ui.importer

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher

actual val scanUsesCamera = true

@Composable
actual fun rememberScanLauncher(onCaptured: (List<PlatformFile>) -> Unit): () -> Unit {
    val launcher = rememberCameraPickerLauncher { photo -> photo?.let { onCaptured(listOf(it)) } }
    return launcher::launch
}
