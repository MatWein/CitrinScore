package de.citrinscore.score.audio

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import androidx.annotation.RequiresPermission
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.context
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CompletableFuture

private const val WAV_HEADER_SIZE = 44
private const val RIFF_CHUNK_HEADER_SIZE = 8
private const val FMT_CHUNK_SIZE = 16
private const val PCM_FORMAT: Short = 1
private const val BITS_PER_BYTE = 8
private const val BUFFER_FACTOR = 2

actual suspend fun isMicrophoneAvailable() = FileKit.context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

@RequiresPermission(Manifest.permission.RECORD_AUDIO)
actual suspend fun startAudioRecording(target: PlatformFile): AudioRecording = withContext(Dispatchers.IO) {
    val bufferSize = AudioRecord.getMinBufferSize(RECORDING_SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
    val record = AudioRecord(
        rawMicrophoneSource(),
        RECORDING_SAMPLE_RATE,
        AudioFormat.CHANNEL_IN_MONO,
        AudioFormat.ENCODING_PCM_16BIT,
        bufferSize * BUFFER_FACTOR,
    )
    check(record.state == AudioRecord.STATE_INITIALIZED) { "Microphone is not available" }
    val output = RandomAccessFile(target.path, "rw").apply {
        setLength(0)
        write(ByteArray(WAV_HEADER_SIZE))
    }
    record.startRecording()
    val writing = CompletableFuture.runAsync {
        val buffer = ByteArray(bufferSize)
        while (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            val read = record.read(buffer, 0, buffer.size)
            if (read > 0) output.write(buffer, 0, read)
        }
    }
    AndroidRecording(record, output, writing)
}

private class AndroidRecording(
    private val record: AudioRecord,
    private val output: RandomAccessFile,
    private val writing: CompletableFuture<Void>,
) : AudioRecording {
    override suspend fun finish() {
        record.stop()
        writing.await()
        withContext(Dispatchers.IO) {
            output.seek(0)
            output.write(wavHeader(dataSize = (output.length() - WAV_HEADER_SIZE).toInt()))
            output.close()
        }
        record.release()
    }

    override fun cancel() {
        record.stop()
        writing.join()
        output.close()
        record.release()
    }
}

private fun wavHeader(dataSize: Int): ByteArray {
    val blockAlign = RECORDING_CHANNELS * RECORDING_BITS_PER_SAMPLE / BITS_PER_BYTE
    return ByteBuffer.allocate(WAV_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        .put("RIFF".encodeToByteArray()).putInt(WAV_HEADER_SIZE - RIFF_CHUNK_HEADER_SIZE + dataSize).put("WAVE".encodeToByteArray())
        .put("fmt ".encodeToByteArray()).putInt(FMT_CHUNK_SIZE).putShort(PCM_FORMAT).putShort(RECORDING_CHANNELS.toShort())
        .putInt(RECORDING_SAMPLE_RATE).putInt(RECORDING_SAMPLE_RATE * blockAlign).putShort(blockAlign.toShort())
        .putShort(RECORDING_BITS_PER_SAMPLE.toShort())
        .put("data".encodeToByteArray()).putInt(dataSize)
        .array()
}
