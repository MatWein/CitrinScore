package de.citrinscore.score.playback

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine

private const val BITS_PER_SAMPLE = 16
private const val BUFFER_MILLIS = 100
private const val MILLIS_PER_SECOND = 1000

internal actual fun openPcmOutput(sampleRate: Int, channels: Int): PcmOutput {
    val format = AudioFormat(sampleRate.toFloat(), BITS_PER_SAMPLE, channels, true, false)
    val line = AudioSystem.getSourceDataLine(format)
    line.open(format, sampleRate * BUFFER_MILLIS / MILLIS_PER_SECOND * format.frameSize)
    line.start()
    return JavaSoundOutput(line)
}

private class JavaSoundOutput(private val line: SourceDataLine) : PcmOutput {
    private var bytes = ByteBuffer.allocate(0).order(ByteOrder.LITTLE_ENDIAN)

    override val playedFrames get() = line.longFramePosition

    override fun write(samples: ShortArray) {
        if (bytes.capacity() != samples.size * Short.SIZE_BYTES) bytes = ByteBuffer.allocate(samples.size * Short.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        bytes.clear()
        bytes.asShortBuffer().put(samples)
        line.write(bytes.array(), 0, bytes.capacity())
    }

    override fun close() {
        line.stop()
        line.flush()
        line.close()
    }
}
