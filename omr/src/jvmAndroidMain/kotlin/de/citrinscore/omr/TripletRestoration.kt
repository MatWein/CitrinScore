package de.citrinscore.omr

import org.apache.commons.numbers.fraction.Fraction
import java.util.IdentityHashMap

private const val MAX_THREES_IN_A_ROW = 4
private const val MIN_TRIPLETS_ON_A_PAGE = 3
private val MAX_MEASURE_LENGTH = Fraction.of(2)
private val PLAIN = Regex("(note|rest)_(4|8|16|32|64)")

private data class TimedNote(val voice: String, val duration: Fraction, val index: Int)

private data class Three(val indices: Set<Int>, val offBeat: Boolean)

private data class Breaks(val count: Int, val size: Fraction) : Comparable<Breaks> {
    override fun compareTo(other: Breaks) = compareValuesBy(this, other, { it.count }, { it.size })
}

private typealias Measure = List<List<TimedNote>>
private typealias ThreesByPlace = LinkedHashMap<Pair<List<Int>, String>, Three>

private val indexOrder = Comparator<List<Int>> { a, b ->
    a.zip(b).firstOrNull { (x, y) -> x != y }?.let { (x, y) -> x.compareTo(y) } ?: a.size.compareTo(b.size)
}

private fun asTriplet(symbol: EncodedSymbol): EncodedSymbol {
    val match = PLAIN.matchEntire(symbol.rhythm) ?: return symbol
    return symbol.copy(rhythm = "${match.groupValues[1]}_${match.groupValues[2].toInt() * 3 / 2}")
}

private fun splitIntoMeasures(symbols: List<EncodedSymbol>): List<Measure> {
    val indexOf = IdentityHashMap<EncodedSymbol, Int>().apply { symbols.forEachIndexed { index, symbol -> put(symbol, index) } }
    val measures = mutableListOf<MutableList<List<TimedNote>>>(mutableListOf())
    for (chord in sortTokenChords(symbols)) {
        val rhythm = chord[0].rhythm
        if (chord[0].isBarlineOrRepeat) {
            measures += mutableListOf<List<TimedNote>>()
        } else if (chord[0].isNoteOrRest && !rhythm.endsWith("m")) {
            measures.last() += chord.filter { it.isNoteOrRest }.map { TimedNote(it.position, it.duration.fraction, indexOf.getValue(it)) }
        }
    }
    return measures
}

private class Played(val start: Fraction, val notes: List<Pair<String, Fraction>>)

private fun play(measure: Measure, triplets: Set<Int> = emptySet()): List<Played> {
    var clock = Fraction.ZERO
    var sounding = listOf<Fraction>()
    return measure.map { chord ->
        val notes = chord.mapNotNull { note ->
            val played = if (note.index in triplets) note.duration.multiply(2).divide(3) else note.duration
            if (played > Fraction.ZERO) note.voice to clock.add(played) else null
        }
        Played(clock, notes).also {
            if (notes.isNotEmpty()) {
                sounding = sounding + notes.map { it.second }
                val current = clock
                clock = sounding.filter { end -> end > current }.min()
                val now = clock
                sounding = sounding.filter { end -> end > now }
            }
        }
    }
}

private fun countBreaks(measure: Measure, triplets: Set<Int> = emptySet()): Breaks {
    val endOfVoice = LinkedHashMap<String, Fraction>()
    var breaks = 0
    var size = Fraction.ZERO
    for (played in play(measure, triplets)) {
        val ends = LinkedHashMap<String, Fraction>()
        played.notes.forEach { (voice, end) -> ends[voice] = maxOf(ends[voice] ?: end, end) }
        for (voice in ends.keys) {
            val previous = endOfVoice[voice]
            if (previous != null && previous != played.start) {
                breaks += 1
                size = size.add(played.start.subtract(previous).abs())
            }
        }
        endOfVoice.putAll(ends)
    }
    if (endOfVoice.isNotEmpty()) {
        val endOfMeasure = endOfVoice.values.max()
        endOfVoice.values.filter { it != endOfMeasure }.forEach {
            breaks += 1
            size = size.add(endOfMeasure.subtract(it))
        }
    }
    return Breaks(breaks, size)
}

private fun measureLength(measure: Measure) = play(measure).flatMap { played -> played.notes.map { it.second } }.maxOrNull() ?: Fraction.ZERO

private fun findSingleThrees(measure: Measure, symbols: List<EncodedSymbol>): ThreesByPlace {
    val found = LinkedHashMap<Pair<List<Int>, String>, Pair<MutableSet<Int>, Boolean>>()
    for (voice in measure.flatMap { chord -> chord.map { it.voice } }.toSortedSet()) {
        val chords = measure.mapIndexed { place, chord -> place to chord.filter { it.voice == voice }.map { it.index } }.filter { it.second.isNotEmpty() }
        val values = chords.map { (_, notes) ->
            val matches = notes.map { PLAIN.matchEntire(symbols[it].rhythm) }
            val plain = matches.filterNotNull().map { it.groupValues[2] }.toSet()
            if (matches.all { it != null } && plain.size == 1) plain.single() else null
        }
        var startOfRun = 0
        values.forEachIndexed { i, value ->
            if (i > 0 && (value == null || value != values[i - 1])) startOfRun = i
            if (value == null || values.subList(i, minOf(i + 3, values.size)) != List(3) { value }) return@forEachIndexed
            val inVoice = chords.subList(i, i + 3).flatMap { it.second }
            if (inVoice.none { symbols[it].rhythm.startsWith("note") }) return@forEachIndexed
            val key = chords.subList(i, i + 3).map { it.first } to value
            val (indices, offBeat) = found[key] ?: (mutableSetOf<Int>() to false)
            indices += inVoice
            found[key] = indices to (offBeat || (i - startOfRun) % 3 != 0)
        }
    }
    return found.mapValuesTo(ThreesByPlace()) { (_, value) -> Three(value.first.toSet(), value.second) }
}

private fun addFollowingThree(inARow: ThreesByPlace, single: ThreesByPlace, measure: Measure, symbols: List<EncodedSymbol>): ThreesByPlace {
    val longer = ThreesByPlace()
    for ((key, three) in inARow) {
        val (places, value) = key
        val voices = three.indices.map { symbols[it].position }.toSet()
        val nextPlace = (places.last() + 1 until measure.size).firstOrNull { place -> measure[place].any { it.voice in voices } }
        for ((nextKey, next) in single) {
            val (nextPlaces, nextValue) = nextKey
            if (nextValue == value && nextPlaces[0] == nextPlace && (three.indices intersect next.indices).isEmpty() &&
                next.indices.map { symbols[it].position }.toSet() == voices
            ) {
                longer[places + nextPlaces to value] = Three(three.indices + next.indices, three.offBeat)
            }
        }
    }
    return longer
}

private fun findThrees(measure: Measure, symbols: List<EncodedSymbol>): List<Three> {
    val single = findSingleThrees(measure, symbols)
    val threes = LinkedHashMap<Set<Int>, Boolean>()
    single.values.forEach { threes[it.indices] = it.offBeat }
    var inARow = single
    repeat(MAX_THREES_IN_A_ROW - 1) {
        inARow = addFollowingThree(inARow, single, measure, symbols)
        inARow.values.forEach { threes.putIfAbsent(it.indices, it.offBeat) }
    }
    return threes.map { (indices, offBeat) -> Three(indices, offBeat) }.sortedWith(compareBy(indexOrder) { it.indices.sorted() })
}

private fun chooseTriplets(measure: Measure, symbols: List<EncodedSymbol>): List<Set<Int>> {
    val chosen = mutableListOf<Set<Int>>()
    var triplets = emptySet<Int>()
    var breaks = countBreaks(measure)
    if (breaks.count == 0 || measureLength(measure) > MAX_MEASURE_LENGTH) return emptyList()
    val threes = findThrees(measure, symbols)
    while (true) {
        val trials = threes.filter { (it.indices intersect triplets).isEmpty() }.map { three -> countBreaks(measure, triplets + three.indices) to three }
        if (trials.isEmpty()) break
        val (bestBreaks, best) = trials.minWithOrNull(
            compareBy<Pair<Breaks, Three>> { it.first }.thenBy { it.second.offBeat }.then(compareBy(indexOrder) { it.second.indices.sorted() }),
        )!!
        if (bestBreaks.count >= breaks.count) break
        chosen += best.indices
        triplets = triplets + best.indices
        breaks = bestBreaks
    }
    return chosen
}

internal fun restoreUnmarkedTriplets(symbols: List<EncodedSymbol>): List<EncodedSymbol> {
    val chosen = splitIntoMeasures(symbols).map { chooseTriplets(it, symbols) }
    fun noteValue(indices: Set<Int>) = symbols[indices.min()].rhythm.split("_")[1]
    val shown = chosen.flatten().groupingBy(::noteValue).eachCount()
    val triplets = chosen.flatten().filter { (shown[noteValue(it)] ?: 0) >= MIN_TRIPLETS_ON_A_PAGE }.flatten().toSet()
    return symbols.mapIndexed { index, symbol -> if (index in triplets) asTriplet(symbol) else symbol }
}
