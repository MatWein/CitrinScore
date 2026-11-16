package de.citrinscore.ui.viewer.audio

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMicrophoneAccess(onGranted: () -> Unit): () -> Unit = onGranted
