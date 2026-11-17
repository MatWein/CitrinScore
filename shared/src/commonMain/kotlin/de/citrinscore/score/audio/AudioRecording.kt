package de.citrinscore.score.audio

import io.github.vinceglb.filekit.PlatformFile

internal const val RECORDING_SAMPLE_RATE = 48000
internal const val RECORDING_BITS_PER_SAMPLE = 16
internal const val RECORDING_CHANNELS = 1

interface AudioRecording {
    suspend fun finish()

    fun cancel()
}

expect suspend fun startAudioRecording(target: PlatformFile): AudioRecording

expect suspend fun isMicrophoneAvailable(): Boolean
