package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.MusicXmlEngraverTest.Companion.note
import de.citrinscore.score.musicxml.MusicXmlEngraverTest.Companion.score
import de.citrinscore.score.musicxml.openMusicXmlEngraver
import dev.atsushieno.ktmidi.Midi1Music
import io.github.vinceglb.filekit.FileKit
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.read
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val PIANO = Instrument.Piano.program

class MidiProgramsTest {

    private val filesDir = Files.createTempDirectory("citrinscore-files").toFile()

    @BeforeTest
    fun setUp() {
        FileKit.init(appId = "citrinscore-test", filesDir = filesDir)
    }

    @AfterTest
    fun tearDown() {
        filesDir.deleteRecursively()
    }

    @Test
    fun `the instrument replaces every program of the performance`() = runTest {
        val midi = openMusicXmlEngraver(score(measures = 2).encodeToByteArray(), compressed = false).use { it.perform() }.midi
        val violin = midi.withLeadProgram(Instrument.Violin.program)

        assertEquals(Instrument.Violin.program, violin.initialProgram())
        assertEquals(setOf(Instrument.Violin.program), violin.messages(MidiChannelStatus.PROGRAM).map { it.first }.toSet())
        assertEquals(midi.messages(MidiChannelStatus.NOTE_ON), violin.messages(MidiChannelStatus.NOTE_ON))
    }

    @Test
    fun `the instrument only replaces the program of the first part`() = runTest {
        val midi = openMusicXmlEngraver(duet().encodeToByteArray(), compressed = false).use { it.perform() }.midi
        val violin = midi.withLeadProgram(Instrument.Violin.program)

        assertEquals(setOf(Instrument.Violin.program, PIANO), violin.messages(MidiChannelStatus.PROGRAM).map { it.first }.toSet())
        assertEquals(Instrument.Violin.program, violin.initialProgram())
        assertEquals(midi.messages(MidiChannelStatus.NOTE_ON), violin.messages(MidiChannelStatus.NOTE_ON))
    }

    private fun duet() = buildString {
        append("""<?xml version="1.0" encoding="UTF-8"?><score-partwise version="4.0"><part-list>""")
        append("""<score-part id="P1"><part-name>Voice</part-name><midi-instrument id="P1-I1"><midi-channel>1</midi-channel><midi-program>54</midi-program></midi-instrument></score-part>""")
        append("""<score-part id="P2"><part-name>Piano</part-name><midi-instrument id="P2-I1"><midi-channel>2</midi-channel><midi-program>${PIANO + 1}</midi-program></midi-instrument></score-part>""")
        append("</part-list>")
        listOf("P1", "P2").forEach { part ->
            append("""<part id="$part"><measure number="1"><attributes><divisions>1</divisions><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>""")
            append(listOf("C", "D", "E", "F").joinToString("") { note(it) })
            append("</measure></part>")
        }
        append("</score-partwise>")
    }

    private fun ByteArray.messages(status: Int) = Midi1Music().apply { read(toList()) }.tracks.flatMap { track ->
        var tick = 0
        track.events.mapNotNull { event ->
            tick += event.deltaTime
            event.message.takeIf { it.statusCode.toInt() and 0xFF == status }?.let { it.msb.toInt() to tick }
        }
    }
}
