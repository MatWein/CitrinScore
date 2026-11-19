package de.citrinscore.omr

private typealias Layout = List<Boolean>

private const val GRANDSTAFF_GAP_FACTOR = 1.5
private const val NEIGHBOR_GAP_SHARE = 0.8
private const val MIN_SYSTEMS_FOR_DROP = 3

private val layoutOrder = Comparator<Layout> { a, b ->
    a.zip(b).firstOrNull { (x, y) -> x != y }?.let { (x, _) -> if (x) 1 else -1 } ?: a.size.compareTo(b.size)
}

private fun List<MultiStaff>.flatStaffs() = flatMap { it.staffs }

private fun List<MultiStaff>.isUniform() = map { it.layout }.toSet().size == 1

private fun <T> List<T>.counts(): LinkedHashMap<T, Int> = LinkedHashMap<T, Int>().also { counts -> forEach { counts[it] = (counts[it] ?: 0) + 1 } }

private fun mostCommon(counts: Map<Layout, Int>): Layout? {
    if (counts.isEmpty()) return null
    val ranked = counts.entries.sortedByDescending { it.value }
    if (ranked.size > 1 && ranked[1].value == ranked[0].value) return null
    return ranked[0].key
}

private fun verticalGap(upper: Staff, lower: Staff): Double? {
    val upperPoints = upper.grid.associateBy { roundHalfEven(it.x) }
    val gaps = lower.grid.mapNotNull { point -> upperPoints[roundHalfEven(point.x)]?.let { point.y.first() - it.y.last() } }
    return if (gaps.isEmpty()) null else gaps.median()
}

private fun possibleGrandstaffs(staffs: List<MultiStaff>, strict: Boolean, guessLoneStaffs: Boolean = true): Set<Pair<Staff, Staff>> {
    val flat = staffs.flatStaffs()
    val systemOf = HashMap<Staff, Int>().apply { staffs.forEachIndexed { i, row -> row.staffs.forEach { put(it, i) } } }
    val lines = LINES_ON_A_STAFF
    val grandstaffGaps = flat.filter { it.isGrandstaff }.flatMap { staff ->
        staff.grid.filter { it.y.size == 2 * lines }.map { point -> (point.y[lines] - point.y[lines - 1]) * (lines - 1) / (point.y[lines - 1] - point.y[0]) }
    }
    val maxGap = if (grandstaffGaps.isEmpty()) Double.POSITIVE_INFINITY else GRANDSTAFF_GAP_FACTOR * grandstaffGaps.median()
    val pairs = flat.zipWithNext()
    val measured = pairs.map { (upper, lower) -> verticalGap(upper, lower) }
    val gaps = measured.zip(pairs) { gap, (upper, lower) -> gap ?: (lower.minY - upper.maxY) }
    val result = mutableSetOf<Pair<Staff, Staff>>()
    pairs.forEachIndexed { i, (upper, lower) ->
        val gap = measured[i]
        val unitSize = minOf(upper.averageUnitSize, lower.averageUnitSize)
        if (upper.isGrandstaff || lower.isGrandstaff || gap == null || gap > maxGap * unitSize) return@forEachIndexed
        val neighbors = listOf(i - 1, i + 1).filter { j -> j in gaps.indices && !flat[j].isGrandstaff && !flat[j + 1].isGrandstaff }.map { gaps[it] }
        val connected = systemOf[upper] == systemOf[lower]
        val lone = listOf(upper, lower).all { staffs[systemOf.getValue(it)].staffs.size == 1 }
        val guess = !strict && (guessLoneStaffs || !lone)
        if (guess || connected || neighbors.all { gap < NEIGHBOR_GAP_SHARE * it }) result += upper to lower
    }
    return result
}

private fun fitToLayout(staffs: List<Staff>, layout: Layout, grandstaffs: Set<Pair<Staff, Staff>>): List<Staff>? {
    val result = mutableListOf<Staff>()
    var i = 0
    for (isGrandstaff in layout) {
        if (i < staffs.size && staffs[i].isGrandstaff == isGrandstaff) {
            result += staffs[i]
            i += 1
        } else if (isGrandstaff && i + 1 < staffs.size && !staffs[i].isGrandstaff && !staffs[i + 1].isGrandstaff && (staffs[i] to staffs[i + 1]) in grandstaffs) {
            result += staffs[i].merge(staffs[i + 1])
            i += 2
        } else {
            return null
        }
    }
    return if (i == staffs.size) result else null
}

private fun fitNeighbors(staffs: List<MultiStaff>, start: Int, layout: Layout, grandstaffs: Set<Pair<Staff, Staff>>): Pair<List<Staff>, Int>? {
    for (end in start + 1..staffs.size) {
        val group = staffs.subList(start, end).flatStaffs()
        if (group.size > 2 * layout.size) return null
        if (layout.none { it } && group.size == end - start && end - start > 1) continue
        fitToLayout(group, layout, grandstaffs)?.let { return it to end }
    }
    return null
}

private fun repairSystems(staffs: List<MultiStaff>, layout: Layout, strict: Boolean = false, guessLoneStaffs: Boolean = true): List<MultiStaff> {
    val grandstaffs = possibleGrandstaffs(staffs, strict, guessLoneStaffs)
    val result = mutableListOf<MultiStaff>()
    var i = 0
    while (i < staffs.size) {
        if (staffs[i].layout == layout) {
            result += staffs[i]
            i += 1
            continue
        }
        val row = staffs[i].staffs
        val repeats = row.size / layout.size
        if (repeats > 1 && staffs[i].layout == List(repeats) { layout }.flatten() && !staffs[i].certain) {
            result += row.chunked(layout.size).map { MultiStaff(it) }
            i += 1
            continue
        }
        val fit = fitNeighbors(staffs, i, layout, grandstaffs)
        if (fit == null) {
            result += staffs[i]
            i += 1
        } else {
            result += MultiStaff(fit.first)
            i = fit.second
        }
    }
    return result
}

private fun repairScore(staffs: List<MultiStaff>, layout: Layout): Double {
    val systemOf = HashMap<Staff, MultiStaff>().apply { staffs.forEach { row -> row.staffs.forEach { put(it, row) } } }
    return repairSystems(staffs, layout, strict = true).filter { it.layout == layout }.sumOf { row ->
        val contradicts = row.staffs.map { systemOf[it] }.any { source -> source == null || !row.staffs.containsAll(source.staffs) }
        (row.staffs.size + layout.count { it }) * if (contradicts) 0.5 else 1.0
    }
}

private fun mixedLayouts(staffs: List<MultiStaff>): LinkedHashMap<Layout, Int> {
    val mixed = mutableListOf<Layout>()
    staffs.forEach { row -> if (row.layout.toSet().size > 1) mixed += row.layout }
    staffs.zipWithNext().forEach { (upper, lower) ->
        val pair = upper.layout + lower.layout
        if (pair == listOf(false, true) || pair == listOf(true, false)) mixed += pair
    }
    return mixed.counts()
}

private fun isShownByMixedSystems(staffs: List<MultiStaff>, layout: Layout): Boolean {
    val mixed = mixedLayouts(staffs)
    return mostCommon(mixed) == layout && (mixed[layout] ?: 0) > 1
}

private fun commonLayout(staffs: List<MultiStaff>): Layout? {
    val mixed = mixedLayouts(staffs)
    val mixedLayout = mostCommon(mixed)
    if (mixedLayout != null && (mixed[mixedLayout] ?: 0) > 1) return mixedLayout
    val counts = staffs.map { it.layout }.counts()
    val candidates = (counts.keys + mixed.keys).sortedWith(layoutOrder)
        .filterNot { layout -> layout.size > 1 && layout.all { it } && (counts[layout] ?: 0) < 2 }
    return candidates.maxWithOrNull(compareBy<Layout> { repairScore(staffs, it) }.thenBy { counts[it] ?: 0 })
}

private fun dropSingleStaffAtEnd(staffs: List<MultiStaff>, layout: Layout): List<MultiStaff> {
    val single = staffs.filter { it.layout == listOf(false) }
    val onlyGrandstaffs = false !in layout
    if (!onlyGrandstaffs || single != listOf(staffs.last()) || staffs.size < MIN_SYSTEMS_FOR_DROP) return staffs
    return staffs.dropLast(1)
}

private fun findPeriodicCore(flat: List<Staff>): Triple<Int, Int, Int>? {
    val layout = flat.map { it.isGrandstaff }
    val n = layout.size
    var best: List<Int>? = null
    for (period in 1..n / 2) {
        for (frontTrim in 0..period) {
            for (backTrim in 0..period) {
                if (frontTrim > n - backTrim) continue
                val core = layout.subList(frontTrim, n - backTrim)
                if (core.size < 2 * period || core.size % period != 0) continue
                val rows = core.chunked(period)
                if (!rows.all { it == rows[0] }) continue
                val candidate = listOf(frontTrim + backTrim, period, frontTrim, backTrim)
                val current = best
                if (current == null || candidate[0] < current[0] || (candidate[0] == current[0] && candidate[1] < current[1])) best = candidate
            }
        }
    }
    return best?.let { Triple(it[1], it[2], it[3]) }
}

private fun regroupByPattern(staffs: List<MultiStaff>): List<MultiStaff>? {
    val flat = staffs.flatStaffs()
    val (period, frontTrim, backTrim) = findPeriodicCore(flat) ?: return null
    val end = flat.size - backTrim
    val pattern = flat.subList(frontTrim, frontTrim + period).map { it.isGrandstaff }.toSet()
    val trimmed = frontTrim > 0 || backTrim > 0
    val nothingGrouped = staffs.all { it.staffs.size == 1 }
    if (pattern.size != 2 || (trimmed && !nothingGrouped)) return null
    return flat.subList(0, frontTrim).map { MultiStaff(listOf(it)) } +
        (frontTrim until end step period).map { MultiStaff(flat.subList(it, it + period)) } +
        flat.subList(end, flat.size).map { MultiStaff(listOf(it)) }
}

private fun convertToLayout(row: MultiStaff, layout: Layout): MultiStaff? {
    if (row.layout == layout) return row
    val single = row.staffs.flatMap { staff ->
        if (staff.isGrandstaff) staff.split()?.toList() ?: return null else listOf(staff)
    }
    val remaining = single.iterator()
    val staffs = layout.map { isGrandstaff ->
        val staff = remaining.next()
        if (isGrandstaff) {
            val lower = remaining.next()
            verticalGap(staff, lower) ?: return null
            staff.merge(lower)
        } else {
            staff
        }
    }
    return MultiStaff(staffs, row.certain)
}

private fun unifyGrandStaffs(staffs: List<MultiStaff>): List<MultiStaff> {
    val staffsPerSystem = staffs.map { row -> row.staffs.sumOf { if (it.isGrandstaff) 2 else 1 } }.toSet()
    if (staffsPerSystem.size != 1) return staffs
    val layout = mostCommon(staffs.map { it.layout }.counts()) ?: return staffs
    return staffs.map { convertToLayout(it, layout) ?: return staffs }
}

internal fun repairSystems(staffs: List<MultiStaff>): List<MultiStaff> {
    val unified = unifyGrandStaffs(staffs)
    if (unified.isUniform() && unified[0].staffs.size > 1) return unified
    val layout = commonLayout(unified)
    val repaired = if (layout == null) unified else repairSystems(unified, layout, guessLoneStaffs = isShownByMixedSystems(unified, layout))
    if (!repaired.isUniform() && unified.none { it.certain }) {
        regroupByPattern(unified)?.let { return it }
    }
    return if (layout == null) repaired else dropSingleStaffAtEnd(repaired, layout)
}
