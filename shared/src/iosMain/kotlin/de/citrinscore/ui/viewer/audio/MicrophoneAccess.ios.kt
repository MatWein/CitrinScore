package de.citrinscore.ui.viewer.audio

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioSession

@Composable
actual fun rememberMicrophoneAccess(onGranted: () -> Unit): () -> Unit {
    val scope = rememberCoroutineScope()
    val granted by rememberUpdatedState(onGranted)
    return { AVAudioSession.sharedInstance().requestRecordPermission { allowed -> if (allowed) scope.launch { granted() } } }
}
