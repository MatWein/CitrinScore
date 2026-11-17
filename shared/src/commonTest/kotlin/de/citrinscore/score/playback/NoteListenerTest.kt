package de.citrinscore.score.playback

import de.citrinscore.score.playback.PitchDetectorTest.Companion.SAMPLE_RATE
import de.citrinscore.score.playback.PitchDetectorTest.Companion.tone
import kotlin.math.pow
import kotlin.test.Test
import kotlin.test.assertEquals

class NoteListenerTest {

    @Test
    fun `each new note is reported once when it starts`() {
        val listener = NoteListener(SAMPLE_RATE)
        val heard = listOf(note(69), note(71), note(71, amplitude = 0.4f), silence(), note(69), note(72)).flatMap { chunked(it).flatMap(listener::listen) }

        assertEquals(listOf(69, 71, 69, 72), heard)
    }

    @Test
    fun `repeated notes are told apart by a dip in volume`() {
        val listener = NoteListener(SAMPLE_RATE)
        val heard = listOf(note(67), note(67, amplitude = 0.05f, seconds = 0.15f), note(67)).flatMap { chunked(it).flatMap(listener::listen) }

        assertEquals(listOf(67, 67), heard)
    }

    @Test
    fun `quiet input is ignored`() {
        val listener = NoteListener(SAMPLE_RATE)

        assertEquals(emptyList(), chunked(note(69, amplitude = 0.001f)).flatMap(listener::listen))
    }

    private fun note(pitch: Int, amplitude: Float = 0.5f, seconds: Float = 0.4f) =
        tone(440f * 2f.pow((pitch - 69) / 12f), (SAMPLE_RATE * seconds).toInt(), amplitude)

    private fun silence() = FloatArray(SAMPLE_RATE / 4)

    private fun chunked(samples: FloatArray) = samples.toList().chunked(CHUNK) { it.toFloatArray() }

    private companion object {
        const val CHUNK = 441
    }
}
