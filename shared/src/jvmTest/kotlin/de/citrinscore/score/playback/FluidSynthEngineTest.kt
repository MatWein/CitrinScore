package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.MusicXmlEngraverTest.Companion.score
import de.citrinscore.score.musicxml.openMusicXmlEngraver
import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class FluidSynthEngineTest {

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
    fun `the performance is rendered with the sound font from the requested position`() = runTest(timeout = 2.minutes) {
        FluidSynthEngine.open(performance(), instrument = null).use { engine ->
            engine.start(START)
            val loudest = engine.renderBlocks()

            assertTrue(engine.isPlaying)
            assertTrue(loudest > 1000, "loudest sample $loudest")
            assertTrue(engine.currentTick in START + 1..START + TICKS_PER_QUARTER, "tick ${engine.currentTick}")
        }
    }

    @Test
    fun `playback continues from a new position after stopping`() = runTest(timeout = 2.minutes) {
        FluidSynthEngine.open(performance(), instrument = null).use { engine ->
            engine.start(0)
            engine.renderBlocks()
            engine.stop()
            engine.renderBlocks()
            engine.start(START)
            engine.renderBlocks()

            assertTrue(engine.currentTick in START + 1..START + TICKS_PER_QUARTER, "tick ${engine.currentTick}")
        }
    }

    @Test
    fun `single notes sound without disturbing later playback`() = runTest(timeout = 2.minutes) {
        FluidSynthEngine.open(performance(), Instrument.Violin).use { engine ->
            engine.noteOn(A4)
            val loudest = engine.renderBlocks()
            engine.noteOff(A4)
            engine.stop()
            engine.start(START)
            engine.renderBlocks()

            assertTrue(loudest > 1000, "loudest sample $loudest")
            assertTrue(engine.currentTick in START + 1..START + TICKS_PER_QUARTER, "tick ${engine.currentTick}")
        }
    }

    private suspend fun performance() = openMusicXmlEngraver(score(measures = 4).encodeToByteArray(), compressed = false).use { it.perform() }

    private fun FluidSynthEngine.renderBlocks(): Int {
        val samples = ShortArray(BLOCK_FRAMES * ENGINE_CHANNELS)
        return (1..BLOCKS).maxOf {
            render(samples)
            samples.maxOf { sample -> abs(sample.toInt()) }
        }
    }

    private companion object {
        const val TICKS_PER_QUARTER = 120
        const val START = TICKS_PER_QUARTER * 4
        const val BLOCK_FRAMES = 1024
        const val BLOCKS = 20
        const val A4 = 69
    }
}
