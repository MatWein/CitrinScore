package de.citrinscore.score.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

private const val CHUNK_SAMPLES = 512
private val logger = LoggerFactory.getLogger("de.citrinscore.score.audio.MicrophoneStream")

actual fun microphoneSamples(sampleRate: Int): Flow<FloatArray> = flow {
    val format = AudioFormat(sampleRate.toFloat(), RECORDING_BITS_PER_SAMPLE, RECORDING_CHANNELS, true, false)
    val line = AudioSystem.getTargetDataLine(format).apply {
        open(format)
        start()
    }
    logger.debug("Opened microphone {} with {}", line.lineInfo, line.format)
    try {
        val bytes = ByteArray(CHUNK_SAMPLES * format.frameSize)
        while (currentCoroutineContext().isActive) {
            val read = line.read(bytes, 0, bytes.size)
            val shorts = ByteBuffer.wrap(bytes, 0, read).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
            emit(FloatArray(shorts.remaining()) { shorts.get(it) / SAMPLE_SCALE })
        }
    } finally {
        line.stop()
        line.close()
        logger.debug("Closed microphone")
    }
}.flowOn(Dispatchers.IO)
