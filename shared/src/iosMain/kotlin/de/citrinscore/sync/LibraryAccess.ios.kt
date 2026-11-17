package de.citrinscore.sync

import androidx.compose.runtime.Composable

@Composable
actual fun rememberLibraryAccess(onResult: (granted: Boolean) -> Unit): () -> Unit = { onResult(true) }
