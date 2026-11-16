package de.citrinscore.score.audio

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class AudioPlayerTest {

    private val root = Files.createTempDirectory("citrinscore-audio").toFile()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `wav files report their duration and can be positioned`() = runTest {
        val wav = silentWav(seconds = 2)

        openAudioPlayer(PlatformFile(wav)).use { player ->
            assertEquals(2.seconds, player.duration)
            assertFalse(player.isPlaying)

            player.seekTo(1500.milliseconds)

            assertEquals(1500.milliseconds, player.position)
        }
    }

    private fun silentWav(seconds: Int): File {
        val format = AudioFormat(RECORDING_SAMPLE_RATE.toFloat(), RECORDING_BITS_PER_SAMPLE, RECORDING_CHANNELS, true, false)
        val frames = RECORDING_SAMPLE_RATE * seconds
        val stream = AudioInputStream(ByteArrayInputStream(ByteArray(frames * format.frameSize)), format, frames.toLong())
        return File(root, "silence.wav").also { AudioSystem.write(stream, AudioFileFormat.Type.WAVE, it) }
    }
}
