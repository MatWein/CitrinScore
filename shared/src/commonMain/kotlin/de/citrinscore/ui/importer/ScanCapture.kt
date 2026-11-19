package de.citrinscore.ui.importer

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

@Composable
expect fun rememberCameraLauncher(onCaptured: (PlatformFile) -> Unit): (() -> Unit)?

expect val importPickerExtensions: List<String>?
