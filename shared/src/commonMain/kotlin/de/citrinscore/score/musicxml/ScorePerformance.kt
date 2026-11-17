package de.citrinscore.score.musicxml

import kotlin.math.abs

class ScorePerformance(val midi: ByteArray, val ticksPerQuarter: Int, val events: List<PerformanceEvent>) {
    val end = events.maxOfOrNull { event -> event.notes.maxOf { it.end } } ?: 0.0

    private val longestNote = events.maxOfOrNull { event -> event.notes.maxOf { it.end - it.start } } ?: 0.0

    fun eventAt(quarter: Double): Int {
        val index = events.binarySearch { it.quarter.compareTo(quarter) }
        return if (index >= 0) index else -index - 2
    }

    fun soundingAt(quarter: Double): List<PerformedNote> {
        val last = eventAt(quarter)
        if (last < 0) return emptyList()
        val first = (last downTo 0).takeWhile { events[it].quarter >= quarter - longestNote }.lastOrNull() ?: last
        return (first..last).flatMap { events[it].notes }.filter { quarter < it.end }
    }

    fun startOf(note: String): Double? = events.firstOrNull { event -> event.notes.any { it.id == note } }?.quarter
}

data class PerformanceEvent(val quarter: Double, val notes: List<PerformedNote>, val pitches: Set<Int>) {
    val needsWrittenPitches get() = notes.size > 1 || pitches.size != 1

    fun pitchOf(note: String, written: Map<String, Int>): Int? {
        if (!needsWrittenPitches) return pitches.single()
        val own = written[note] ?: return null
        val sounding = pitches.sorted()
        val ranked = notes.mapNotNull { written[it.id] }.sorted()
        return when {
            sounding.isEmpty() -> own
            ranked.size == sounding.size -> sounding[ranked.indexOf(own)]
            else -> sounding.minBy { abs(it - own) }
        }
    }
}

data class PerformedNote(val id: String, val start: Double, val end: Double)
