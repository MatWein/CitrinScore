package de.citrinscore.sync

import androidx.compose.runtime.Composable

@Composable
expect fun rememberLibraryAccess(onResult: (granted: Boolean) -> Unit): () -> Unit
