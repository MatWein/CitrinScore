package de.citrinscore.score.playback

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PitchDetectorTest {

    private val detector = PitchDetector(SAMPLE_RATE, windowSize = 1024)

    @Test
    fun `frequencies across the range of violin and piano are detected`() {
        listOf(55f, 196f, 440f, 659.3f, 2093f).forEach { frequency ->
            val detected = detector.frequency(tone(frequency, detector.frameSize))
            assertTrue(detected != null && abs(detected - frequency) < frequency * 0.005f, "$frequency detected as $detected")
        }
    }

    @Test
    fun `harmonics do not shift the detected fundamental`() {
        val frame = FloatArray(detector.frameSize) { index ->
            (1..6).sumOf { harmonic -> sin(2 * PI * 196.0 * harmonic * index / SAMPLE_RATE) / harmonic }.toFloat() * 0.3f
        }
        val detected = detector.frequency(frame)
        assertTrue(detected != null && abs(detected - 196f) < 1f, "detected $detected")
    }

    @Test
    fun `noise has no pitch`() {
        val random = Random(1)
        assertNull(detector.frequency(FloatArray(detector.frameSize) { random.nextFloat() * 2 - 1 }))
    }

    companion object {
        const val SAMPLE_RATE = 22050

        fun tone(frequency: Float, samples: Int, amplitude: Float = 0.5f) =
            FloatArray(samples) { amplitude * sin(2 * PI * frequency * it / SAMPLE_RATE).toFloat() }
    }
}
