package de.citrinscore.score.playback

import de.citrinscore.score.audio.openAudioPlayer
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

private const val TICKS_PER_QUARTER = 480

class MidiAudioPlayerTest {

    private val filesDir = Files.createTempDirectory("citrinscore-files").toFile()

    @BeforeTest
    fun setUp() {
        FileKit.init(appId = "citrinscore-test", filesDir = filesDir)
    }

    @AfterTest
    fun cleanUp() {
        filesDir.deleteRecursively()
    }

    @Test
    fun `the timeline follows tempo changes across tracks`() {
        val timeline = MidiTimeline(twoTempos())

        assertEquals(TICKS_PER_QUARTER * 8, timeline.totalTicks)
        assertEquals(6.seconds, timeline.duration)
        assertEquals(1.seconds, timeline.timeAt(TICKS_PER_QUARTER * 2))
        assertEquals(4.seconds, timeline.timeAt(TICKS_PER_QUARTER * 6))
        assertEquals(TICKS_PER_QUARTER * 6, timeline.tickAt(4.seconds))
        assertEquals(TICKS_PER_QUARTER * 8, timeline.tickAt(1.minutes))
    }

    @Test
    fun `midi attachments report their duration and can be positioned`() = runTest(timeout = 2.minutes) {
        val file = File(filesDir, "melody.mid").apply { writeBytes(twoTempos()) }

        openAudioPlayer(PlatformFile(file)).use { player ->
            assertEquals(6.seconds, player.duration)
            assertFalse(player.isPlaying)

            player.seekTo(4500.milliseconds)

            assertEquals(4500.milliseconds, player.position)
        }
    }

    /** One quarter note per beat, four beats at 120 bpm in the tempo track, then four at 60 bpm. */
    private fun twoTempos(): ByteArray {
        val quarter = vlq(TICKS_PER_QUARTER)
        val tempos = byteArrayOf(0, *tempo(500_000)) + vlq(TICKS_PER_QUARTER * 4) + tempo(1_000_000) + vlq(TICKS_PER_QUARTER * 4) + END_OF_TRACK
        val notes = (0 until 8).fold(ByteArray(0)) { bytes, _ -> bytes + byteArrayOf(0, 0x90.toByte(), 69, 100) + quarter + byteArrayOf(0x80.toByte(), 69, 0) } +
            byteArrayOf(0) + END_OF_TRACK
        return ByteArrayOutputStream().apply {
            write("MThd".encodeToByteArray())
            write(byteArrayOf(0, 0, 0, 6, 0, 1, 0, 2, (TICKS_PER_QUARTER shr 8).toByte(), TICKS_PER_QUARTER.toByte()))
            listOf(tempos, notes).forEach { track ->
                write("MTrk".encodeToByteArray())
                write(byteArrayOf((track.size shr 24).toByte(), (track.size shr 16).toByte(), (track.size shr 8).toByte(), track.size.toByte()))
                write(track)
            }
        }.toByteArray()
    }

    private fun tempo(microsPerQuarter: Int) =
        byteArrayOf(0xFF.toByte(), 0x51, 3, (microsPerQuarter shr 16).toByte(), (microsPerQuarter shr 8).toByte(), microsPerQuarter.toByte())

    private fun vlq(value: Int): ByteArray {
        var bytes = byteArrayOf((value and 0x7F).toByte())
        var rest = value shr 7
        while (rest > 0) {
            bytes = byteArrayOf(((rest and 0x7F) or 0x80).toByte()) + bytes
            rest = rest shr 7
        }
        return bytes
    }

    private companion object {
        val END_OF_TRACK = byteArrayOf(0xFF.toByte(), 0x2F, 0)
    }
}
