package de.citrinscore.score.audio

import android.Manifest
import android.content.Context
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.annotation.RequiresPermission
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive

@RequiresPermission(Manifest.permission.RECORD_AUDIO)
actual fun microphoneSamples(sampleRate: Int): Flow<FloatArray> = flow {
    val bufferSize = AudioRecord.getMinBufferSize(sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    val record = AudioRecord(rawMicrophoneSource(), sampleRate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize)
    check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone is not available" }
    record.startRecording()
    try {
        val samples = ShortArray(bufferSize / Short.SIZE_BYTES)
        while (currentCoroutineContext().isActive) {
            val read = record.read(samples, 0, samples.size)
            if (read > 0) emit(FloatArray(read) { samples[it] / SAMPLE_SCALE })
        }
    } finally {
        record.stop()
        record.release()
    }
}.flowOn(Dispatchers.IO)

internal fun rawMicrophoneSource(): Int {
    val audio = FileKit.context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val unprocessed = audio.getProperty(AudioManager.PROPERTY_SUPPORT_AUDIO_SOURCE_UNPROCESSED).toBoolean()
    return if (unprocessed) MediaRecorder.AudioSource.UNPROCESSED else MediaRecorder.AudioSource.VOICE_RECOGNITION
}
