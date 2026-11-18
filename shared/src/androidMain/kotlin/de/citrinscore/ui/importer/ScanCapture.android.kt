package de.citrinscore.ui.importer

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher

actual val scanUsesCamera = true

@Composable
actual fun isScanAvailable() = LocalContext.current.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)

@Composable
actual fun rememberScanLauncher(onCaptured: (List<PlatformFile>) -> Unit): () -> Unit {
    val launcher = rememberCameraPickerLauncher { photo -> photo?.let { onCaptured(listOf(it)) } }
    return launcher::launch
}
