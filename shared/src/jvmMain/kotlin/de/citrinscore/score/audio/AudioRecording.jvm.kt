package de.citrinscore.score.audio

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import java.util.concurrent.CompletableFuture
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.TargetDataLine

private val RecordingFormat = AudioFormat(RECORDING_SAMPLE_RATE.toFloat(), RECORDING_BITS_PER_SAMPLE, RECORDING_CHANNELS, true, false)

actual suspend fun isMicrophoneAvailable(): Boolean = withContext(Dispatchers.IO) {
    runCatching { openRecordingLine().close() }.isSuccess
}

actual suspend fun startAudioRecording(target: PlatformFile): AudioRecording = withContext(Dispatchers.IO) {
    val line = openRecordingLine().apply { start() }
    val writing = CompletableFuture.runAsync { AudioSystem.write(AudioInputStream(line), AudioFileFormat.Type.WAVE, target.file) }
    JavaSoundRecording(line, writing)
}

private fun openRecordingLine(): TargetDataLine = AudioSystem.getTargetDataLine(RecordingFormat).apply { open(RecordingFormat) }

private class JavaSoundRecording(private val line: TargetDataLine, private val writing: CompletableFuture<Void>) : AudioRecording {
    override suspend fun finish() {
        cancel()
        writing.await()
    }

    override fun cancel() {
        line.stop()
        line.close()
    }
}
