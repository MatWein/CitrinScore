package de.citrinscore.ui.importer

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.compose.rememberCameraPickerLauncher

@Composable
actual fun rememberCameraLauncher(onCaptured: (PlatformFile) -> Unit): (() -> Unit)? {
    val launcher = rememberCameraPickerLauncher { photo -> photo?.let(onCaptured) }
    val hasCamera = LocalContext.current.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    return if (hasCamera) ({ launcher.launch() }) else null
}

actual val importPickerExtensions: List<String>? = null
