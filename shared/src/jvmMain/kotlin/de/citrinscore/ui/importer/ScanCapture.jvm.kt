package de.citrinscore.ui.importer

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

@Composable
actual fun rememberCameraLauncher(onCaptured: (PlatformFile) -> Unit): (() -> Unit)? = null

actual val importPickerExtensions: List<String>? = SourceKind.entries.flatMap { it.extensions }
