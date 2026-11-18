package de.citrinscore.ui.viewer.audio

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

@Composable
actual fun OnAppBackground(action: () -> Unit) = LifecycleEventEffect(Lifecycle.Event.ON_STOP, onEvent = action)
