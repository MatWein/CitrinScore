package de.citrinscore.score.audio

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@OptIn(ExperimentalForeignApi::class)
actual suspend fun openAudioPlayer(file: PlatformFile): AudioPlayer =
    IosAudioPlayer(checkNotNull(AVAudioPlayer(contentsOfURL = file.nsUrl, error = null)) { "Could not open ${file.nsUrl}" })

private class IosAudioPlayer(private val player: AVAudioPlayer) : AudioPlayer {
    init {
        player.prepareToPlay()
    }

    override val duration get() = player.duration.seconds

    override val position get() = player.currentTime.seconds

    override val isPlaying get() = player.playing

    override fun play() {
        player.play()
    }

    override fun pause() = player.pause()

    override fun seekTo(position: Duration) {
        player.currentTime = position.toDouble(DurationUnit.SECONDS)
    }

    override fun close() = player.stop()
}
