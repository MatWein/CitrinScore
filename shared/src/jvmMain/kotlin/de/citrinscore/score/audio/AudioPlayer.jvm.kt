package de.citrinscore.score.audio

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.DurationUnit

private const val PCM_BITS_PER_SAMPLE = 16
private const val BITS_PER_BYTE = 8

actual suspend fun openAudioPlayer(file: PlatformFile): AudioPlayer = withContext(Dispatchers.IO) {
    val clip = AudioSystem.getClip()
    AudioSystem.getAudioInputStream(file.file).use { stream -> stream.decoded().use(clip::open) }
    ClipAudioPlayer(clip)
}

private fun AudioInputStream.decoded(): AudioInputStream {
    if (format.encoding == AudioFormat.Encoding.PCM_SIGNED || format.encoding == AudioFormat.Encoding.PCM_UNSIGNED) return this
    val frameSize = format.channels * PCM_BITS_PER_SAMPLE / BITS_PER_BYTE
    val pcm = AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.sampleRate, PCM_BITS_PER_SAMPLE, format.channels, frameSize, format.sampleRate, false)
    val samples = AudioSystem.getAudioInputStream(pcm, this).use { it.readAllBytes() }
    return AudioInputStream(samples.inputStream(), pcm, (samples.size / frameSize).toLong())
}

private class ClipAudioPlayer(private val clip: Clip) : AudioPlayer {
    @Volatile
    override var isPlaying = false
        private set

    init {
        clip.addLineListener { event -> if (event.type == LineEvent.Type.STOP) isPlaying = false }
    }

    override val duration get() = clip.microsecondLength.microseconds

    override val position get() = clip.microsecondPosition.microseconds

    override fun play() {
        if (clip.framePosition >= clip.frameLength) clip.framePosition = 0
        isPlaying = true
        clip.start()
    }

    override fun pause() = clip.stop()

    override fun seekTo(position: Duration) {
        clip.framePosition = (position.toDouble(DurationUnit.SECONDS) * clip.format.frameRate).roundToInt()
    }

    override fun close() = clip.close()
}
