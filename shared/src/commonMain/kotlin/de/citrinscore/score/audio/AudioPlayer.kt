package de.citrinscore.score.audio

import io.github.vinceglb.filekit.PlatformFile
import kotlin.time.Duration

interface AudioPlayer : AutoCloseable {
    val duration: Duration
    val position: Duration
    val isPlaying: Boolean

    fun play()

    fun pause()

    fun seekTo(position: Duration)
}

expect suspend fun openAudioPlayer(file: PlatformFile): AudioPlayer
