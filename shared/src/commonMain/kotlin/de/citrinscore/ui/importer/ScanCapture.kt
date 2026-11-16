package de.citrinscore.ui.importer

import androidx.compose.runtime.Composable
import io.github.vinceglb.filekit.PlatformFile

expect val scanUsesCamera: Boolean

@Composable
expect fun isScanAvailable(): Boolean

@Composable
expect fun rememberScanLauncher(onCaptured: (List<PlatformFile>) -> Unit): () -> Unit
