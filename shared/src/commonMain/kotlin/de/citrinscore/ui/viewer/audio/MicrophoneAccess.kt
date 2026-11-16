package de.citrinscore.ui.viewer.audio

import androidx.compose.runtime.Composable

@Composable
expect fun rememberMicrophoneAccess(onGranted: () -> Unit): () -> Unit
