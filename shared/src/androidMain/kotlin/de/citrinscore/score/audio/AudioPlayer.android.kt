package de.citrinscore.score.audio

import android.media.MediaPlayer
import de.citrinscore.files.openReadOnlyDescriptor
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

actual suspend fun openAudioPlayer(file: PlatformFile): AudioPlayer = withContext(Dispatchers.IO) {
    val player = MediaPlayer()
    file.openReadOnlyDescriptor().use { player.setDataSource(it.fileDescriptor) }
    player.prepare()
    MediaPlayerAudioPlayer(player)
}

private class MediaPlayerAudioPlayer(private val player: MediaPlayer) : AudioPlayer {
    override val duration get() = player.duration.milliseconds

    override val position get() = player.currentPosition.milliseconds

    override val isPlaying get() = player.isPlaying

    override fun play() {
        if (position >= duration) player.seekTo(0)
        player.start()
    }

    override fun pause() = player.pause()

    override fun seekTo(position: Duration) = player.seekTo(position.inWholeMilliseconds, MediaPlayer.SEEK_PREVIOUS_SYNC)

    override fun close() = player.release()
}
