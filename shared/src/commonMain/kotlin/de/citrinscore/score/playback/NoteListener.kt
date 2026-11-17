package de.citrinscore.score.playback

import kotlin.math.log2
import kotlin.math.roundToInt
import kotlin.math.sqrt

private const val WINDOW_SIZE = 1024
private const val HOP_SIZE = 512
private const val SILENCE_LEVEL = 0.004f
private const val STABLE_FRAMES = 2
private const val REATTACK_DIP = 0.6f
private const val REATTACK_RISE = 1.8f
private const val A4_FREQUENCY = 440f
private const val A4_PITCH = 69
private const val SEMITONES_PER_OCTAVE = 12

class NoteListener(sampleRate: Int) {
    private val detector = PitchDetector(sampleRate, WINDOW_SIZE)
    private val buffer = FloatArray(detector.frameSize)
    private var filled = 0
    private var candidate: Int? = null
    private var candidateFrames = 0
    private var sounding: Int? = null
    private var peak = 0f
    private var dip = 0f

    fun listen(samples: FloatArray): List<Int> = buildList {
        samples.forEach { sample ->
            buffer[filled++] = sample
            if (filled == buffer.size) {
                analyze()?.let(::add)
                buffer.copyInto(buffer, 0, HOP_SIZE)
                filled -= HOP_SIZE
            }
        }
    }

    private fun analyze(): Int? {
        val level = sqrt((buffer.size - HOP_SIZE until buffer.size).sumOf { (buffer[it] * buffer[it]).toDouble() }.toFloat() / HOP_SIZE)
        val pitch = if (level < SILENCE_LEVEL) null else detector.frequency(buffer)?.let(::pitchOf)
        if (pitch == null) {
            candidate = null
            sounding = null
            return null
        }
        candidateFrames = if (pitch == candidate) candidateFrames + 1 else 1
        candidate = pitch
        if (candidateFrames < STABLE_FRAMES) return null
        if (pitch != sounding) return attack(pitch, level)
        if (level > peak) {
            peak = level
            dip = level
        }
        dip = minOf(dip, level)
        return if (dip < peak * REATTACK_DIP && level > dip * REATTACK_RISE) attack(pitch, level) else null
    }

    private fun attack(pitch: Int, level: Float): Int {
        sounding = pitch
        peak = level
        dip = level
        return pitch
    }

    private fun pitchOf(frequency: Float) = (A4_PITCH + SEMITONES_PER_OCTAVE * log2(frequency / A4_FREQUENCY)).roundToInt()
}
