package de.citrinscore.score.audio

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioRecorder
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayAndRecord
import platform.AVFAudio.AVFormatIDKey
import platform.AVFAudio.AVLinearPCMBitDepthKey
import platform.AVFAudio.AVLinearPCMIsBigEndianKey
import platform.AVFAudio.AVLinearPCMIsFloatKey
import platform.AVFAudio.AVNumberOfChannelsKey
import platform.AVFAudio.AVSampleRateKey
import platform.AVFAudio.inputAvailable
import platform.AVFAudio.setActive
import platform.CoreAudioTypes.kAudioFormatLinearPCM

actual suspend fun isMicrophoneAvailable() = AVAudioSession.sharedInstance().inputAvailable

@OptIn(ExperimentalForeignApi::class)
actual suspend fun startAudioRecording(target: PlatformFile): AudioRecording {
    AVAudioSession.sharedInstance().apply {
        setCategory(AVAudioSessionCategoryPlayAndRecord, error = null)
        setActive(true, error = null)
    }
    val settings = mapOf<Any?, Any?>(
        AVFormatIDKey to kAudioFormatLinearPCM,
        AVSampleRateKey to RECORDING_SAMPLE_RATE.toDouble(),
        AVNumberOfChannelsKey to RECORDING_CHANNELS,
        AVLinearPCMBitDepthKey to RECORDING_BITS_PER_SAMPLE,
        AVLinearPCMIsFloatKey to false,
        AVLinearPCMIsBigEndianKey to false,
    )
    val recorder = checkNotNull(AVAudioRecorder(uRL = target.nsUrl, settings = settings, error = null)) { "Microphone is not available" }
    check(recorder.record()) { "Recording could not be started" }
    return IosRecording(recorder)
}

private class IosRecording(private val recorder: AVAudioRecorder) : AudioRecording {
    override suspend fun finish() = recorder.stop()

    override fun cancel() {
        recorder.stop()
        recorder.deleteRecording()
    }
}
