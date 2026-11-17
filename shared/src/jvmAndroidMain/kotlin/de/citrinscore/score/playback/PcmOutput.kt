package de.citrinscore.score.playback

internal interface PcmOutput : AutoCloseable {
    val playedFrames: Long

    fun write(samples: ShortArray)
}

internal expect fun openPcmOutput(sampleRate: Int, channels: Int): PcmOutput
