package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.PerformanceEvent
import de.citrinscore.score.musicxml.PerformedNote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScoreFollowerTest {

    private val events = listOf(
        event(0.0, 60),
        event(1.0, 62),
        event(2.0),
        event(3.0, 64, 67),
        event(4.0, 65),
        event(5.0, 67),
        event(6.0, 69),
        event(7.0, 71),
    )

    @Test
    fun `the cursor waits for the expected note and skips tied notes`() {
        val follower = ScoreFollower(events, start = 0)

        assertNull(follower.hear(61))
        assertEquals(0, follower.expected)
        assertEquals(0, follower.hear(60))
        assertEquals(1, follower.hear(62))
        assertEquals(3, follower.expected)
    }

    @Test
    fun `any note of a chord matches`() {
        val follower = ScoreFollower(events, start = 3)

        assertEquals(3, follower.hear(67))
        assertEquals(4, follower.expected)
    }

    @Test
    fun `a missed note is skipped when the following one is played`() {
        val follower = ScoreFollower(events, start = 4)

        assertEquals(5, follower.hear(67))
        assertEquals(6, follower.expected)
        assertNull(follower.hear(60))
    }

    @Test
    fun `the last note heard again does not skip ahead`() {
        val follower = ScoreFollower(listOf(event(0.0, 69), event(1.0, 72), event(2.0, 69)), start = 0)

        assertEquals(0, follower.hear(69))
        assertNull(follower.hear(69))
        assertEquals(1, follower.hear(72))
    }

    @Test
    fun `repetitions of the last note are skipped when the next note is played`() {
        val repeated = listOf(event(0.0, 67)) + (1..6).map { event(it.toDouble(), 67) } + event(7.0, 63)
        val follower = ScoreFollower(repeated, start = 0)

        assertEquals(0, follower.hear(67))
        assertEquals(7, follower.hear(63))
    }

    @Test
    fun `notes further ahead than the lookahead are not matched`() {
        val follower = ScoreFollower(events, start = 4)

        assertNull(follower.hear(71))
    }

    @Test
    fun `the follower finishes after the last note`() {
        val follower = ScoreFollower(events, start = 7)

        assertEquals(7, follower.hear(71))
        assertTrue(follower.isFinished)
    }

    private fun event(quarter: Double, vararg pitches: Int) =
        PerformanceEvent(quarter, listOf(PerformedNote("n$quarter", quarter, quarter + 1)), pitches.toSet())
}
