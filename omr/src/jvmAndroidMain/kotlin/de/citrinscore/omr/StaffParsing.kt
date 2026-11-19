package de.citrinscore.omr

import org.opencv.core.Mat

private fun slotsInLayout(row: List<Boolean>, layout: List<Boolean>): List<Int>? {
    val matches = combinations(layout.indices.toList(), row.size).filter { slots -> slots.zip(row).all { (slot, isGrandstaff) -> layout[slot] == isGrandstaff } }
    return matches.singleOrNull()
}

private fun combinations(items: List<Int>, size: Int): List<List<Int>> = when {
    size == 0 -> listOf(emptyList())
    items.size < size -> emptyList()
    else -> combinations(items.drop(1), size - 1).map { listOf(items[0]) + it } + combinations(items.drop(1), size)
}

private fun assignParts(staffs: List<MultiStaff>): List<List<Int>>? {
    val layouts = staffs.map { it.layout }.toSet()
    val largest = layouts.maxBy { it.size }
    if (layouts.any { it.size == largest.size && it != largest }) return null
    return staffs.map { slotsInLayout(it.layout, largest) ?: return null }
}

private fun leadingSigns(symbols: List<EncodedSymbol>, prefixes: List<String>): List<EncodedSymbol> {
    val result = mutableListOf<EncodedSymbol>()
    for (symbol in symbols) {
        if (symbol.rhythm.startsWith("note") || symbol.rhythm.startsWith("rest") || symbol.rhythm.startsWith("measureRest") || "barline" in symbol.rhythm) break
        if (prefixes.none { symbol.rhythm.startsWith(it) }) continue
        val last = result.lastOrNull()
        if (last != null && prefixes.any { last.rhythm.startsWith(it) } && (last.rhythm.startsWith("clef") || symbol.rhythm.startsWith("clef"))) {
            result += EncodedSymbol("chord")
        }
        result += symbol
    }
    return result
}

private fun measureRestsLike(symbols: List<EncodedSymbol>, isGrandstaff: Boolean, clefs: List<EncodedSymbol>?): List<EncodedSymbol> {
    val positions = if (isGrandstaff) listOf("upper", "lower") else listOf("upper")
    val result = clefs.orEmpty().toMutableList()
    result += leadingSigns(symbols, listOf("keySignature", "timeSignature")).filter { it.rhythm != "chord" }
    var inMeasure = false
    for (symbol in symbols) {
        if ((symbol.rhythm.startsWith("note") || symbol.rhythm.startsWith("rest")) && !inMeasure) {
            positions.forEachIndexed { i, position ->
                if (i > 0) result += EncodedSymbol("chord")
                result += EncodedSymbol("measureRest", position = position)
            }
            inMeasure = true
        } else if ("barline" in symbol.rhythm || "repeat" in symbol.rhythm) {
            result += EncodedSymbol(symbol.rhythm)
            inMeasure = false
        }
    }
    return result
}

private val keyOrder = compareBy<Pair<Int, Int>>({ it.first }, { it.second })

internal fun parseStaffs(systems: List<MultiStaff>, page: Mat, transformer: Transformer, onStaff: (Int, Int) -> Unit): List<List<EncodedSymbol>> {
    var staffs = systems
    var parts = staffs.map { row -> row.staffs.indices.toList() }
    if (staffs.map { it.staffs.size }.toSet().size > 1) {
        val assigned = assignParts(staffs)
        if (assigned == null) {
            staffs = staffs.flatMap { it.breakApart() }
            parts = List(staffs.size) { listOf(0) }
        } else {
            parts = assigned
        }
    }
    val numberOfVoices = staffs.maxOf { it.staffs.size }
    val layout = staffs.first { it.staffs.size == numberOfVoices }.layout
    val regions = StaffRegions(staffs)
    val jobs = (0 until numberOfVoices).flatMap { voice -> staffs.indices.filter { voice in parts[it] }.map { it to voice } }
    val results = HashMap<Pair<Int, Int>, List<EncodedSymbol>>()
    jobs.forEachIndexed { done, (staffIndex, voice) ->
        onStaff(done, jobs.size)
        val staff = staffs[staffIndex].staffs[parts[staffIndex].indexOf(voice)]
        val predicted = prepareStaffImage(staff, page, regions).use { transformer.predict(it) }
        val result = if (staff.isGrandstaff) predicted else predicted.filterNot { isLowerPosition(it.position) }
        if (result.isNotEmpty()) results[staffIndex to voice] = result
    }
    return (0 until numberOfVoices).map { voice ->
        val symbols = mutableListOf<EncodedSymbol>()
        parts.forEachIndexed { staffIndex, slots ->
            var staffSymbols = results[staffIndex to voice]
            val shown = slots.mapNotNull { results[staffIndex to it] }
            if (voice !in slots && shown.isNotEmpty()) {
                val clefs = results.keys.sortedWith(keyOrder).firstOrNull { it.second == voice }?.let { leadingSigns(results.getValue(it), listOf("clef")) }
                staffSymbols = measureRestsLike(shown[0], layout[voice], clefs)
            }
            if (staffSymbols != null) {
                symbols += staffSymbols
                symbols += EncodedSymbol("newline")
            }
        }
        restoreUnmarkedTriplets(removeDuplicatedSymbols(symbols))
    }
}
