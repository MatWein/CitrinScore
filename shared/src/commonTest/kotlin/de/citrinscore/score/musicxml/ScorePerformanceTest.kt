package de.citrinscore.score.musicxml

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScorePerformanceTest {

    private val performance = ScorePerformance(
        midi = ByteArray(0),
        ticksPerQuarter = 120,
        events = listOf(
            PerformanceEvent(0.0, listOf(PerformedNote("whole", 0.0, 4.0), PerformedNote("a", 0.0, 1.0)), setOf(60, 67)),
            PerformanceEvent(1.0, listOf(PerformedNote("b", 1.0, 2.0)), setOf(69)),
            PerformanceEvent(2.0, listOf(PerformedNote("c", 2.0, 4.0)), setOf(71)),
        ),
    )

    @Test
    fun `the current event is the last one that started`() {
        assertEquals(-1, performance.eventAt(-0.5))
        assertEquals(0, performance.eventAt(0.0))
        assertEquals(1, performance.eventAt(1.5))
        assertEquals(2, performance.eventAt(10.0))
    }

    @Test
    fun `sounding notes include long notes that started earlier`() {
        assertEquals(listOf("whole", "b"), performance.soundingAt(1.5).map { it.id })
        assertEquals(listOf("whole", "c"), performance.soundingAt(3.0).map { it.id })
        assertEquals(emptyList(), performance.soundingAt(4.0).map { it.id })
    }

    @Test
    fun `notes are found by their id`() {
        assertEquals(1.0, performance.startOf("b"))
        assertEquals(4.0, performance.end)
    }

    @Test
    fun `single notes sound at their performed pitch`() {
        val event = PerformanceEvent(0.0, listOf(PerformedNote("a", 0.0, 1.0)), setOf(69))

        assertFalse(event.needsWrittenPitches)
        assertEquals(69, event.pitchOf("a", emptyMap()))
    }

    @Test
    fun `chord notes are matched to performed pitches by their written order`() {
        val event = PerformanceEvent(0.0, listOf(PerformedNote("high", 0.0, 1.0), PerformedNote("low", 0.0, 1.0)), setOf(58, 65))
        val written = mapOf("high" to 67, "low" to 60)

        assertTrue(event.needsWrittenPitches)
        assertEquals(65, event.pitchOf("high", written))
        assertEquals(58, event.pitchOf("low", written))
    }

    @Test
    fun `notes without a matching performed pitch fall back to the nearest or written pitch`() {
        val unison = PerformanceEvent(0.0, listOf(PerformedNote("a", 0.0, 1.0), PerformedNote("b", 0.0, 1.0), PerformedNote("c", 0.0, 1.0)), setOf(60, 64))
        val tied = PerformanceEvent(1.0, listOf(PerformedNote("held", 0.0, 2.0)), emptySet())

        assertEquals(64, unison.pitchOf("c", mapOf("a" to 60, "b" to 60, "c" to 63)))
        assertEquals(62, tied.pitchOf("held", mapOf("held" to 62)))
        assertNull(tied.pitchOf("held", emptyMap()))
    }
}
