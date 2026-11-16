package de.citrinscore.score.audio

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds

actual suspend fun openAudioPlayer(file: PlatformFile): AudioPlayer = withContext(Dispatchers.IO) {
    val clip = AudioSystem.getClip()
    AudioSystem.getAudioInputStream(file.file).use(clip::open)
    ClipAudioPlayer(clip)
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
        clip.microsecondPosition = position.inWholeMicroseconds
    }

    override fun close() = clip.close()
}
