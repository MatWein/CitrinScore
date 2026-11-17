package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.PerformanceEvent

private const val LOOKAHEAD = 3

class ScoreFollower(private val events: List<PerformanceEvent>, start: Int) {
    var expected: Int = playableFrom(start)
        private set

    private var lastPitch: Int? = null

    val isFinished get() = expected >= events.size

    fun hear(pitch: Int): Int? {
        val hit = candidates(pitch).firstOrNull { pitch in events[it].pitches } ?: return null
        lastPitch = pitch
        expected = playableFrom(hit + 1)
        return hit
    }

    private fun candidates(pitch: Int): Sequence<Int> {
        val playable = generateSequence(expected) { playableFrom(it + 1) }.takeWhile { it < events.size }
        if (pitch == lastPitch) return playable.take(1)
        var skipped = 0
        return playable.takeWhile { index ->
            val repeated = lastPitch in events[index].pitches
            if (!repeated) skipped++
            repeated || skipped <= LOOKAHEAD
        }
    }

    private fun playableFrom(index: Int): Int {
        var current = index.coerceAtLeast(0)
        while (current < events.size && events[current].pitches.isEmpty()) current++
        return current
    }
}
