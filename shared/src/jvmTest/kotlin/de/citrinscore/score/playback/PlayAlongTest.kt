package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.MusicXmlEngraverTest.Companion.note
import de.citrinscore.score.musicxml.MusicXmlEngraverTest.Companion.score
import de.citrinscore.score.musicxml.openMusicXmlEngraver
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

class PlayAlongTest {

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
    fun `the follower keeps up with the synthesized performance`() = runTest(timeout = 2.minutes) {
        val melody = listOf("A", "A", "C", "B").joinToString("") { step -> note(step) }
        val performance = openMusicXmlEngraver(score(measures = 3, notes = melody).encodeToByteArray(), compressed = false).use { it.perform() }
        val follower = ScoreFollower(performance.events, start = 0)
        val listener = NoteListener(ENGINE_SAMPLE_RATE / DECIMATION)
        val heard = mutableListOf<Int>()
        FluidSynthEngine.open(performance, instrument = null).use { engine ->
            engine.start(0)
            val samples = ShortArray(BLOCK_FRAMES * ENGINE_CHANNELS)
            while (engine.isPlaying && !follower.isFinished) {
                engine.render(samples)
                listener.listen(samples.toMicrophone()).forEach { pitch -> follower.hear(pitch)?.let(heard::add) }
            }
        }

        assertEquals(performance.events.indices.toList(), heard)
    }

    private fun ShortArray.toMicrophone() = FloatArray(size / ENGINE_CHANNELS / DECIMATION) { index ->
        val first = index * ENGINE_CHANNELS * DECIMATION
        (first until first + ENGINE_CHANNELS * DECIMATION).sumOf { this[it].toDouble() }.toFloat() / (ENGINE_CHANNELS * DECIMATION * Short.MAX_VALUE)
    }

    private companion object {
        const val DECIMATION = 2
        const val BLOCK_FRAMES = 1024
    }
}
