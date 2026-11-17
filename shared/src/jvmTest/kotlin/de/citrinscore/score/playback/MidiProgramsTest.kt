package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.MusicXmlEngraverTest.Companion.score
import de.citrinscore.score.musicxml.openMusicXmlEngraver
import dev.atsushieno.ktmidi.Midi1Music
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.read
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MidiProgramsTest {

    @Test
    fun `the instrument replaces every program of the performance`() = runTest {
        val midi = openMusicXmlEngraver(score(measures = 2).encodeToByteArray(), compressed = false).use { it.perform() }.midi
        val violin = midi.withProgram(Instrument.Violin.program)

        assertEquals(Instrument.Violin.program, violin.initialProgram())
        assertEquals(setOf(Instrument.Violin.program), violin.messages(MidiChannelStatus.PROGRAM).map { it.first }.toSet())
        assertEquals(midi.messages(MidiChannelStatus.NOTE_ON), violin.messages(MidiChannelStatus.NOTE_ON))
    }

    private fun ByteArray.messages(status: Int) = Midi1Music().apply { read(toList()) }.tracks.flatMap { track ->
        var tick = 0
        track.events.mapNotNull { event ->
            tick += event.deltaTime
            event.message.takeIf { it.statusCode.toInt() and 0xFF == status }?.let { it.msb.toInt() to tick }
        }
    }
}
