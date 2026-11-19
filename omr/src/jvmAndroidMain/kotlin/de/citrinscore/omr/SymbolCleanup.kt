package de.citrinscore.omr

import org.apache.commons.numbers.fraction.Fraction

private const val LOWER_CLEF_WINDOW = 5

internal fun removeDuplicatedSymbols(symbols: List<EncodedSymbol>): List<EncodedSymbol> {
    val chords = keepLowerStaffOnlyWithClef(fixOverEagerTuplets(groupIntoChords(symbols)))
    return flattenChords(removeRedundantSignatures(chords.map(::removeDuplicatedPitches)))
}

private fun groupIntoMeasures(chords: List<List<EncodedSymbol>>): List<List<List<EncodedSymbol>>> {
    val measures = mutableListOf<List<List<EncodedSymbol>>>()
    var current = mutableListOf<List<EncodedSymbol>>()
    for (chord in chords) {
        current += chord
        if (chord.isNotEmpty() && chord[0].isBarlineOrRepeat) {
            measures += current
            current = mutableListOf()
        }
    }
    if (current.isNotEmpty()) measures += current
    return measures
}

private fun measureDuration(measure: List<List<EncodedSymbol>>): Fraction = measure.fold(Fraction.ZERO) { total, chord ->
    val shortest = chord.filter { it.isNoteOrRest }.map { it.duration.fraction }.filter { it > Fraction.ZERO }.minOrNull() ?: Fraction.ZERO
    total.add(shortest)
}

private fun fixOverEagerTuplets(chords: List<List<EncodedSymbol>>): List<List<EncodedSymbol>> {
    val measures = groupIntoMeasures(chords)
    val durations = measures.map(::measureDuration)
    val typical = if (durations.isEmpty()) Fraction.ZERO else durations.sorted()[durations.size / 2]
    return measures.zip(durations).flatMap { (measure, duration) ->
        if (duration < typical) measure.map { chord -> chord.map { it.removeTuplet() } } else measure
    }
}

private fun keepLowerStaffOnlyWithClef(chords: List<List<EncodedSymbol>>): List<List<EncodedSymbol>> {
    var hasLowerClef = false
    var lineStart = 0
    return chords.mapIndexed { i, chord ->
        if (chord.any { it.rhythm == "newline" }) {
            hasLowerClef = false
            lineStart = i + 1
        }
        chord.map { symbol ->
            when {
                hasLowerClef -> symbol
                i - lineStart < LOWER_CLEF_WINDOW && symbol.rhythm.startsWith("clef") && isLowerPosition(symbol.position) -> {
                    hasLowerClef = true
                    symbol
                }
                else -> symbol.toUpperPosition()
            }
        }
    }
}

private fun removeDuplicatedPitches(chord: List<EncodedSymbol>): List<EncodedSymbol> {
    if (chord.size <= 1 || !chord[0].isNoteOrRest) return chord
    return chord.distinctBy { "${it.pitch} ${it.position}" }
}

private fun removeRedundantSignatures(chords: List<List<EncodedSymbol>>): List<List<EncodedSymbol>> {
    var clefUpper = ""
    var clefLower = ""
    var key = ""
    var time = ""
    return chords.map { chord ->
        chord.filter { symbol ->
            when {
                symbol.rhythm.startsWith("clef") && !isLowerPosition(symbol.position) -> (symbol.rhythm != clefUpper).also { if (it) clefUpper = symbol.rhythm }
                symbol.rhythm.startsWith("clef") -> (symbol.rhythm != clefLower).also { if (it) clefLower = symbol.rhythm }
                symbol.rhythm.startsWith("keySignature") -> (symbol.rhythm != key).also { if (it) key = symbol.rhythm }
                symbol.rhythm.startsWith("timeSignature") -> (symbol.rhythm != time).also { if (it) time = symbol.rhythm }
                else -> true
            }
        }
    }
}

private fun flattenChords(chords: List<List<EncodedSymbol>>): List<EncodedSymbol> = chords.flatMap { chord ->
    chord.flatMapIndexed { index, symbol -> if (index > 0) listOf(EncodedSymbol("chord"), symbol) else listOf(symbol) }
}
