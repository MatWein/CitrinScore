package de.citrinscore.omr

import kotlin.math.abs

private const val STEPS = "CDEFGAB"
private val CLEF_REFERENCES = mapOf('G' to diatonic('G', 4), 'F' to diatonic('F', 3), 'C' to diatonic('C', 4))
private val DEFAULT_UPPER_CLEF = Clef('G', 2)
private val DEFAULT_LOWER_CLEF = Clef('F', 4)
private val PITCH = Regex("([A-G])(\\d)")

private data class Clef(val sign: Char, val line: Int)

private fun diatonic(step: Char, octave: Int) = STEPS.indexOf(step) + 7 * octave

private fun clefOf(rhythm: String): Clef? {
    val signAndLine = rhythm.split("_").getOrNull(1) ?: return null
    val sign = signAndLine.firstOrNull()?.takeIf { it in CLEF_REFERENCES } ?: return null
    val line = signAndLine.drop(1).toIntOrNull() ?: return null
    return Clef(sign, line)
}

internal fun placeOnStaffLines(symbols: List<EncodedSymbol>, staff: Staff): List<EncodedSymbol> {
    val clefs = mutableMapOf(false to DEFAULT_UPPER_CLEF, true to DEFAULT_LOWER_CLEF)
    return symbols.map { symbol ->
        val lower = isLowerPosition(symbol.position)
        if (symbol.rhythm.startsWith("clef")) clefOf(symbol.rhythm)?.let { clefs[lower] = it }
        val location = symbol.location
        val pitch = PITCH.matchEntire(symbol.pitch)
        if (location == null || pitch == null || !symbol.rhythm.startsWith("note")) return@map symbol
        val y = lineY(staff, location.x, lower, clefs.getValue(lower), diatonic(pitch.groupValues[1][0], pitch.groupValues[2].toInt())) ?: return@map symbol
        symbol.copy(location = location.copy(lineY = y))
    }
}

private fun lineY(staff: Staff, x: Double, lower: Boolean, clef: Clef, note: Int): Double? {
    val point = staff.grid.minBy { abs(it.x - x) }
    val lines = when {
        !lower -> point.y.take(LINES_ON_A_STAFF)
        point.y.size == 2 * LINES_ON_A_STAFF -> point.y.drop(LINES_ON_A_STAFF)
        else -> return null
    }
    val bottom = lines.last()
    val halfSpace = (bottom - lines.first()) / (2 * (LINES_ON_A_STAFF - 1))
    val steps = 2 * (clef.line - 1) + note - CLEF_REFERENCES.getValue(clef.sign)
    return bottom - steps * halfSpace
}

private const val ZONE_UNITS = 5.0
private const val COLUMN_UNITS = 0.5
private const val MAX_HORIZONTAL_UNITS = 20.0
private const val MAX_VERTICAL_UNITS = 3.0
private const val HORIZONTAL_WEIGHT = 0.1
private const val SKIP_NOTE_COST = 2.5
private const val SKIP_HEAD_COST = 0.3

internal fun alignToNoteheads(symbols: List<EncodedSymbol>, staff: Staff, noteheads: List<BoundingEllipse>): List<EncodedSymbol> {
    val unit = staff.averageUnitSize
    val heads = noteheads.filter { head ->
        val point = staff.grid.minBy { abs(it.x - head.box.centerX) }
        head.box.centerX >= staff.minX - ZONE_UNITS * unit && head.box.centerX <= staff.maxX + ZONE_UNITS * unit &&
            head.box.centerY >= point.y.first() - ZONE_UNITS * unit && head.box.centerY <= point.y.last() + ZONE_UNITS * unit
    }.sortedWith(compareBy({ Math.round(it.box.centerX / (COLUMN_UNITS * unit)) }, { it.box.centerY }))
    val notes = chordGroups(symbols).flatMap { group ->
        group.filter { symbols[it].rhythm.startsWith("note") && symbols[it].location != null }.sortedBy { symbols[it].location!!.targetY }
    }
    val matches = align(notes.map { symbols[it].location!! }, heads, unit)
    val result = symbols.toMutableList()
    matches.forEach { (note, head) ->
        result[notes[note]] = symbols[notes[note]].copy(location = symbols[notes[note]].location!!.copy(head = heads[head].bounds))
    }
    return result
}

private val SymbolLocation.targetY get() = lineY ?: y

private fun chordGroups(symbols: List<EncodedSymbol>): List<List<Int>> {
    val groups = mutableListOf<MutableList<Int>>()
    var joined = false
    symbols.forEachIndexed { index, symbol ->
        when {
            symbol.rhythm == "chord" -> joined = true
            joined && groups.isNotEmpty() -> {
                groups.last() += index
                joined = false
            }
            else -> groups += mutableListOf(index)
        }
    }
    return groups
}

private fun align(notes: List<SymbolLocation>, heads: List<BoundingEllipse>, unit: Double): List<Pair<Int, Int>> {
    val n = notes.size
    val m = heads.size
    val cost = Array(n + 1) { DoubleArray(m + 1) }
    val step = Array(n + 1) { IntArray(m + 1) }
    for (i in 0..n) {
        for (j in 0..m) {
            if (i == 0 && j == 0) continue
            var best = Double.POSITIVE_INFINITY
            var move = 0
            if (i > 0 && cost[i - 1][j] + SKIP_NOTE_COST < best) {
                best = cost[i - 1][j] + SKIP_NOTE_COST
                move = 1
            }
            if (j > 0 && cost[i][j - 1] + SKIP_HEAD_COST < best) {
                best = cost[i][j - 1] + SKIP_HEAD_COST
                move = 2
            }
            if (i > 0 && j > 0) {
                val match = matchCost(notes[i - 1], heads[j - 1], unit)
                if (cost[i - 1][j - 1] + match < best) {
                    best = cost[i - 1][j - 1] + match
                    move = 3
                }
            }
            cost[i][j] = best
            step[i][j] = move
        }
    }
    val matches = mutableListOf<Pair<Int, Int>>()
    var i = n
    var j = m
    while (i > 0 || j > 0) {
        when (step[i][j]) {
            1 -> i--
            2 -> j--
            else -> {
                matches += i - 1 to j - 1
                i--
                j--
            }
        }
    }
    return matches
}

private fun matchCost(note: SymbolLocation, head: BoundingEllipse, unit: Double): Double {
    val horizontal = abs(head.box.centerX - note.x) / unit
    val vertical = abs(head.box.centerY - note.targetY) / unit
    if (horizontal > MAX_HORIZONTAL_UNITS || vertical > MAX_VERTICAL_UNITS) return Double.POSITIVE_INFINITY
    return HORIZONTAL_WEIGHT * horizontal + vertical
}
