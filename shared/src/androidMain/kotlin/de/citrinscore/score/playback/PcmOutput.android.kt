package de.citrinscore.score.playback

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

private const val BUFFER_FACTOR = 2

internal actual fun openPcmOutput(sampleRate: Int, channels: Int): PcmOutput {
    val mask = if (channels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
    val format = AudioFormat.Builder().setSampleRate(sampleRate).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(mask).build()
    val track = AudioTrack.Builder()
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
        .setAudioFormat(format)
        .setTransferMode(AudioTrack.MODE_STREAM)
        .setBufferSizeInBytes(AudioTrack.getMinBufferSize(sampleRate, mask, AudioFormat.ENCODING_PCM_16BIT) * BUFFER_FACTOR)
        .build()
    track.play()
    return AudioTrackOutput(track)
}

private class AudioTrackOutput(private val track: AudioTrack) : PcmOutput {
    override val playedFrames get() = track.playbackHeadPosition.toUInt().toLong()

    override fun write(samples: ShortArray) {
        track.write(samples, 0, samples.size)
    }

    override fun close() {
        track.pause()
        track.flush()
        track.release()
    }
}
