package de.citrinscore.sync

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context

internal fun hasLibraryAccess(): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        FileKit.context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

@Composable
actual fun rememberLibraryAccess(onResult: (granted: Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val result by rememberUpdatedState(onResult)
    val settings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result(hasLibraryAccess()) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result(it) }
    return {
        when {
            hasLibraryAccess() -> result(true)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
                settings.launch(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, "package:${context.packageName}".toUri()))
            else -> permission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }
}
