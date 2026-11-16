package de.citrinscore.ui.importer

import android.content.pm.PackageManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun isScanAvailable() = LocalContext.current.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
