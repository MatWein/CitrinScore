package de.citrinscore.omr

import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.slf4j.LoggerFactory
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min

private val logger = LoggerFactory.getLogger("de.citrinscore.omr.StaffDetection")

private const val ANCHOR_LEDGER_LINES = 5
private const val MAX_GAP_IN_UNIT_SIZES = 15
private const val STAFF_DENSITY = 10
private const val MAX_PEAK_HEIGHT_RATIO = 1.5
private const val PEAK_DISTANCE_SHARE = 0.7
private const val CLEF_ZONE_MARGIN = 10
private const val MIN_STAFFS_PER_PAGE = 2
private const val WIDE_FRAGMENT_LIMIT = 100
private val SLICES_ACROSS_PAGE = listOf(0.2, 0.35, 0.5, 0.65, 0.8)
private val CLEF_OFFSETS = listOf(-10, 0, 10, 30, 60, 80)
private val BAR_LINE_OFFSETS = listOf(-10, -5, 0, 5, 10)

internal fun makeLinesStronger(image: Mat, width: Int, height: Int): Mat {
    val dilated = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(width.toDouble(), height.toDouble())).use { kernel ->
        Mat().also { Imgproc.dilate(image, it, kernel) }
    }
    return dilated.use { Mat().also { result -> Imgproc.threshold(it, result, 0.1, 1.0, Imgproc.THRESH_BINARY) } }
}

internal class StaffLineSegment(val debugId: Int, fragments: List<RotatedBoundingBox>) {
    val fragments = fragments.sortedBy { it.box.centerX }
    val minX = fragments.minOf { it.box.centerX - it.box.width / 2 }
    val maxX = fragments.maxOf { it.box.centerX + it.box.width / 2 }
    val minY = fragments.minOf { it.box.centerY - it.box.height / 2 }
    val maxY = fragments.maxOf { it.box.centerY + it.box.height / 2 }

    fun merge(other: StaffLineSegment) = StaffLineSegment(debugId, fragments + other.fragments.filter { it !in fragments })

    fun at(x: Double): RotatedBoundingBox? = fragments.firstOrNull {
        x >= it.box.centerX - it.box.width / 2 - STAFF_LINE_SEGMENT_X_TOLERANCE && x <= it.box.centerX + it.box.width / 2 + STAFF_LINE_SEGMENT_X_TOLERANCE
    }

    fun extrapolatedY(x: Double) = (at(x) ?: fragments.first()).centerExtrapolated(x)

    fun contains(other: StaffLineSegment) = fragments.toSet().containsAll(other.fragments)
}

internal class StaffAnchor(val lines: List<StaffLineSegment>, val symbol: RotatedBoundingBox) {
    val averageUnitSize: Double
    val minY = lines.minOf { it.minY }
    val maxY = lines.maxOf { it.maxY }
    val zoneStart: Int
    val zoneStop: Int

    init {
        val positions = lines.map { it.fragments.first().centerExtrapolated(symbol.box.centerX) }.sorted()
        val deltas = positions.zipWithNext { a, b -> abs(b - a) }
        averageUnitSize = if (deltas.isEmpty()) 0.0 else deltas.average()
        zoneStart = (minY - ANCHOR_LEDGER_LINES * averageUnitSize).toInt()
        zoneStop = (maxY + ANCHOR_LEDGER_LINES * averageUnitSize).toInt()
    }
}

internal class RawStaff(val staffId: Int, val lines: List<StaffLineSegment>, val anchors: List<StaffAnchor>) {
    val bounds: RotatedBoundingBox
    val minX: Double
    val maxX: Double
    val minY: Double
    val maxY: Double

    init {
        val contours = lines.flatMap { line -> line.fragments.flatMap { it.contours.asList() } }.toIntArray()
        bounds = RotatedBoundingBox(Box.of(minAreaRect(contours)), contours, staffId)
        minX = bounds.box.centerX - bounds.box.width / 2
        maxX = bounds.box.centerX + bounds.box.width / 2
        minY = bounds.box.centerY - bounds.box.height / 2
        maxY = bounds.box.centerY + bounds.box.height / 2
    }

    fun merge(other: RawStaff) = RawStaff(staffId, lines.indices.map { other.lines[it].merge(lines[it]) }, anchors + other.anchors)

    fun isOverlapping(other: RawStaff) = bounds.isOverlapping(other.bounds)
}

private fun staffForAnchor(anchor: StaffAnchor, staffs: List<RawStaff>) =
    staffs.firstOrNull { staff -> anchor.lines.indices.any { staff.lines[it].contains(anchor.lines[it]) } }

private fun findRawStaffs(anchors: List<StaffAnchor>, fragments: List<RotatedBoundingBox>): List<RawStaff> {
    val staffs = mutableListOf<RawStaff>()
    anchors.forEachIndexed { staffId, anchor ->
        val existing = staffForAnchor(anchor, staffs)
        val inZone = fragments.filter { it.box.centerY >= anchor.zoneStart && it.box.centerY <= anchor.zoneStop }
        val connected = connectStaffLines(inZone, anchor.averageUnitSize)
        val lines = anchor.lines.map { anchorLine ->
            connected.filter { it.contains(anchorLine) }.singleOrNull() ?: anchorLine
        }
        val staff = RawStaff(staffId, lines, listOf(anchor))
        if (existing != null) {
            staffs.remove(existing)
            staffs += existing.merge(staff)
        } else {
            staffs += staff
        }
    }
    return staffs
}

private fun removeDuplicateStaffs(staffs: List<RawStaff>): List<RawStaff> {
    var result = mutableListOf<RawStaff>()
    for (staff in staffs) {
        val overlapping = result.filter { staff.isOverlapping(it) }
        when {
            overlapping.isEmpty() -> result += staff
            overlapping.size >= 2 -> Unit
            overlapping[0].anchors.size < staff.anchors.size -> {
                result = result.filter { it !== overlapping[0] }.toMutableList()
                result += staff
            }
        }
    }
    return result
}

private fun gapHasInk(left: RawStaff, right: RawStaff, ink: Mat): Boolean {
    val x0 = max(left.maxX.toInt(), 0)
    val x1 = right.minX.toInt()
    if (x1 <= x0) return true
    val y0 = max(min(left.minY, right.minY).toInt(), 0)
    val y1 = min(max(left.maxY, right.maxY).toInt(), ink.rows())
    val right1 = min(x1, ink.cols())
    if (right1 <= x0) return false
    if (y1 <= y0) return false
    val columns = ink.region(x0, y0, right1, y1).use { region ->
        Mat().use { reduced ->
            Core.reduce(region, reduced, 0, Core.REDUCE_MAX)
            Core.countNonZero(reduced)
        }
    }
    return columns.toDouble() / (right1 - x0) >= 0.5
}

private fun continues(left: RawStaff, right: RawStaff): Boolean {
    val unitSize = (left.anchors + right.anchors).map { it.averageUnitSize }.average()
    val gap = right.minX - left.maxX
    if (unitSize <= 0 || gap < 0 || gap > MAX_GAP_IN_UNIT_SIZES * unitSize) return false
    val linesWhichLineUp = left.lines.zip(right.lines).count { (leftLine, rightLine) ->
        val leftY = leftLine.fragments.last().centerExtrapolated(leftLine.maxX)
        val rightY = rightLine.fragments.first().centerExtrapolated(rightLine.minX)
        abs(leftY - rightY) <= unitSize / 2
    }
    return linesWhichLineUp >= LINES_ON_A_STAFF - 1
}

private fun mergeStaffsInSameRow(staffs: List<RawStaff>, ink: Mat): List<RawStaff> {
    val result = staffs.sortedBy { it.minX }.toMutableList()
    var i = 0
    while (i < result.size) {
        val right = result.drop(i + 1).firstOrNull { continues(result[i], it) && gapHasInk(result[i], it, ink) }
        if (right == null) {
            i++
            continue
        }
        val merged = result[i].merge(right)
        result.remove(right)
        result[i] = merged
    }
    return result
}

internal fun connectStaffLines(lines: List<RotatedBoundingBox>, unitSize: Double): List<StaffLineSegment> {
    val remaining = lines.sortedByDescending { it.leftX }.toMutableList()
    val result = mutableListOf<MutableList<RotatedBoundingBox>>()
    var active = mutableListOf<MutableList<RotatedBoundingBox>>()
    var lastCleanupAt = 0.0
    while (remaining.isNotEmpty()) {
        val current = remaining.removeAt(remaining.lastIndex)
        val x = current.leftX
        if (x - lastCleanupAt > maxLineGapSize(unitSize)) {
            active = active.filter { x - it.last().rightX < maxLineGapSize(unitSize) }.toMutableList()
            lastCleanupAt = x
        }
        if (current.box.width < isShortLine(unitSize)) continue
        var connected = false
        for (line in active) {
            if (line.last().isOverlappingExtrapolated(current, unitSize)) {
                line += current
                connected = true
            }
        }
        if (!connected) {
            val line = mutableListOf(current)
            result += line
            active += line
        }
    }
    return result.sortedBy { it.first().box.centerY }.mapIndexed { index, fragments -> StaffLineSegment(index, fragments) }
}

private fun areLinesCrossing(lines: List<StaffLineSegment>): Boolean {
    for (i in lines.indices) {
        for (j in i + 1 until lines.size) {
            val start = max(lines[i].minX, lines[j].minX)
            val stop = min(lines[i].maxX, lines[j].maxX)
            if (start >= stop) continue
            val deltaStart = lines[i].extrapolatedY(start) - lines[j].extrapolatedY(start)
            val deltaStop = lines[i].extrapolatedY(stop) - lines[j].extrapolatedY(stop)
            if (deltaStart == 0.0 || deltaStop == 0.0 || (deltaStart > 0) != (deltaStop > 0)) return true
        }
    }
    return false
}

private fun areLinesParallel(lines: List<StaffLineSegment>, unitSize: Double): Boolean {
    val fragments = lines.flatMap { it.fragments }
    if (fragments.isEmpty()) return false
    val averageAngle = fragments.map { it.angle }.average()
    return fragments.none { abs(it.angle - averageAngle) > MAX_ANGLE_FOR_PARALLEL_LINES && it.width > isShortConnectedLine(unitSize) }
}

private fun beginsOrEndsOnStaffLine(symbol: RotatedBoundingBox, lines: List<StaffLineSegment>, unitSize: Double) = lines.any { line ->
    val fragment = line.at(symbol.box.centerX) ?: return@any false
    abs(fragment.centerExtrapolated(symbol.box.centerX) - symbol.box.centerY) < unitSize
}

private fun findStaffAnchors(fragments: List<RotatedBoundingBox>, symbols: List<RotatedBoundingBox>, areClefs: Boolean): List<StaffAnchor> {
    val result = mutableListOf<StaffAnchor>()
    for (center in symbols) {
        val offsets = if (areClefs) CLEF_OFFSETS else BAR_LINE_OFFSETS
        for (symbol in offsets.map { if (it == 0) center else center.moveHorizontallyBy(it) }) {
            val estimatedUnitSize = roundHalfEven(symbol.height / (LINES_ON_A_STAFF - 1))
            val thickened = symbol.makeBoxTaller(estimatedUnitSize)
            val overlapping = fragments.filter { it.isIntersecting(thickened) }
            var connected = connectStaffLines(overlapping, estimatedUnitSize.toDouble())
            if (connected.size > LINES_ON_A_STAFF) {
                connected = connected.filter { it.maxX - it.minX > isShortConnectedLine(estimatedUnitSize.toDouble()) }
            }
            if (connected.size != LINES_ON_A_STAFF) continue
            if (!areLinesParallel(connected, estimatedUnitSize.toDouble())) continue
            if (areLinesCrossing(connected)) continue
            if (!areClefs && !beginsOrEndsOnStaffLine(symbol, connected, estimatedUnitSize.toDouble())) continue
            result += StaffAnchor(connected, symbol)
        }
    }
    return result
}

private fun resampleSegment(anchor: StaffAnchor, staff: RawStaff, axis: List<Int>): List<StaffPoint> {
    val anchorX = anchor.symbol.box.centerX
    val anchorFragments = anchor.lines.map { it.fragments.first() }
    var previous = StaffPoint(anchorX, anchorFragments.map { it.centerExtrapolated(anchorX) }, anchorFragments.map { it.angle }.average())
    val result = mutableListOf<StaffPoint>()
    val half = 0.5 * anchor.averageUnitSize
    for (xValue in axis) {
        val x = xValue.toDouble()
        val lines = staff.lines.map { it.at(x) }
        val centers: MutableList<Double?> = lines.map { it?.centerExtrapolated(x) }.toMutableList()
        val values = centers.filterNotNull()
        if (values.isEmpty()) continue
        values.differences().forEachIndexed { i, delta ->
            if (delta < half) {
                centers[i] = null
                centers[i + 1] = null
            }
        }
        previous.y.forEachIndexed { i, previousY ->
            val center = centers[i]
            if (center != null && abs(center - previousY) > half) centers[i] = null
        }
        var previousCenter = -1
        for (i in centers.indices + centers.indices.reversed()) {
            val center = centers[i]
            if (center != null) {
                previousCenter = i
            } else if (previousCenter >= 0) {
                centers[i] = centers[previousCenter]?.let { it + anchor.averageUnitSize * (i - previousCenter) }
            }
        }
        if (centers.any { it == null }) continue
        previous = StaffPoint(x, centers.filterNotNull(), lines.filterNotNull().map { it.angle }.average())
        result += previous
    }
    return result
}

private fun resampleStaff(staff: RawStaff): Staff {
    val anchors = staff.anchors.sortedBy { it.symbol.box.centerX }
    fun roundToDensity(x: Double) = roundHalfEven(x / STAFF_DENSITY) * STAFF_DENSITY
    val start = floor(staff.minX / STAFF_DENSITY) * STAFF_DENSITY
    val stop = (floor(staff.maxX / STAFF_DENSITY) + 1) * STAFF_DENSITY
    val grid = mutableListOf<StaffPoint>()
    var x = start
    anchors.forEachIndexed { i, anchor ->
        val center = anchor.symbol.box.centerX
        val toLeft = pythonRange(roundToDensity(x), roundToDensity(center), STAFF_DENSITY)
        val (rightStart, rightStop) = if (i < anchors.lastIndex) {
            center.toInt() to ((center + anchors[i + 1].symbol.box.centerX) / 2).toInt()
        } else {
            roundToDensity(center) to roundToDensity(stop)
        }
        x = rightStop.toDouble()
        grid += resampleSegment(anchor, staff, toLeft.reversed()).reversed()
        grid += resampleSegment(anchor, staff, pythonRange(rightStart, rightStop, STAFF_DENSITY))
    }
    return Staff(grid)
}

private fun filterEdgeOfVision(staffs: List<Staff>, width: Int, height: Int): List<Staff> {
    val usualWidth = staffs.map { it.maxX - it.minX }.average()
    return staffs.filter { staff ->
        if (staff.maxY >= height || staff.minY < 0) return@filter false
        val shorterThanUsual = staff.maxX - staff.minX < usualWidth / 2
        val beyondSide = staff.minX < 0.01 * width || staff.maxX > 0.99 * width
        !(beyondSide && shorterThanUsual)
    }
}

private fun mainColumn(extents: List<Pair<Double, Double>>): Pair<Double, Double> {
    val longest = extents.maxOf { (left, right) -> right - left }
    val long = extents.filter { (left, right) -> right - left >= 0.75 * longest }
    return long.map { it.first }.median() to long.map { it.second }.median()
}

private fun shareInColumn(left: Double, right: Double, column: Pair<Double, Double>): Double {
    val inside = min(right, column.second) - max(left, column.first)
    return if (right > left) inside / (right - left) else 0.0
}

private fun leftPage(staffs: List<Staff>): List<Staff> {
    val unit = staffs.map { it.averageUnitSize }.median()
    for (split in staffs.map { it.maxX }.toSortedSet()) {
        val left = staffs.filter { it.maxX <= split }
        val right = staffs.filter { it.maxX > split }
        if (left.size < MIN_STAFFS_PER_PAGE || right.size < MIN_STAFFS_PER_PAGE) continue
        if (right.minOf { it.minX } - split < 2 * unit) continue
        val leftWidth = left.map { it.maxX - it.minX }.median()
        val rightWidth = right.map { it.maxX - it.minX }.median()
        if (leftWidth >= rightWidth / 2) return left
    }
    return staffs
}

private fun filterNeighborPage(staffs: List<Staff>): List<Staff> {
    if (staffs.isEmpty()) return staffs
    val page = leftPage(staffs)
    val column = mainColumn(page.map { it.minX to it.maxX })
    val minWidth = (column.second - column.first) / 2
    return page.filter { it.maxX - it.minX >= minWidth || shareInColumn(it.minX, it.maxX, column) >= 0.5 }
}

private fun filterUnusualAnchors(anchors: List<StaffAnchor>): List<StaffAnchor> {
    if (anchors.isEmpty()) return anchors
    val unitSizes = anchors.map { it.averageUnitSize }
    val average = unitSizes.average()
    val deviation = unitSizes.standardDeviation()
    return anchors.filter { abs(it.averageUnitSize - average) <= 3 * deviation }
}

private fun clefZones(anchors: List<StaffAnchor>, width: Int): List<IntRange> {
    val ranges = anchors.map { anchor ->
        max(anchor.symbol.leftX.toInt(), 0) until min((anchor.symbol.rightX + CLEF_ZONE_MARGIN).toInt(), width)
    }.sortedBy { it.first }
    val result = mutableListOf<IntRange>()
    for (range in ranges) {
        val last = result.lastOrNull()
        if (last != null && range.first < last.last + 1) result[result.lastIndex] = last.first until range.last + 1 else result += range
    }
    return result
}

private fun lineGroups(peaks: List<Int>): List<Int> {
    val gaps = peaks.zipWithNext { a, b -> (b - a).toDouble() }.sorted()
    val count = max(5, roundHalfEven(peaks.size * 0.2))
    val approxUnit = gaps.take(count).average()
    val maxGap = approxUnit * MAX_PEAK_HEIGHT_RATIO
    val extended = listOf(peaks.first() - maxGap - 1) + peaks.map { it.toDouble() }
    var group = -1
    return (1 until extended.size).map { i ->
        if (extended[i] - extended[i - 1] > maxGap) group++
        group
    }
}

private fun findHorizontalLines(image: Mat, unitSize: Double): List<List<Int>> {
    val rows = image.rows()
    val counts = Mat().use { reduced ->
        Mat().use { binary ->
            Imgproc.threshold(image, binary, 0.0, 1.0, Imgproc.THRESH_BINARY)
            Core.reduce(binary, reduced, 1, Core.REDUCE_SUM, org.opencv.core.CvType.CV_32S)
        }
        IntArray(rows).also { reduced.get(0, 0, it) }
    }
    val padded = DoubleArray(rows + 2) { if (it == 0 || it == rows + 1) 0.0 else counts[it - 1].toDouble() }
    val deviation = padded.asList().standardDeviation()
    if (deviation == 0.0) return emptyList()
    val mean = padded.average()
    val norm = DoubleArray(padded.size) { (padded[it] - mean) / deviation }
    val centers = findPeaks(norm, 0.0, PEAK_DISTANCE_SHARE * unitSize, 1.0).map { it - 1 }
    if (centers.size < LINES_ON_A_STAFF) return emptyList()
    val groups = lineGroups(centers)
    val grouped = LinkedHashMap<Int, MutableList<Int>>()
    centers.forEachIndexed { i, center -> grouped.getOrPut(groups[i]) { mutableListOf() } += center }
    return grouped.values.filter { it.size == LINES_ON_A_STAFF }.map { it.sorted() }
}

private fun findFiveLineGroups(image: Mat, zones: List<IntRange>, unitSize: Double): List<RotatedBoundingBox> = zones.flatMap { zone ->
    if (zone.isEmpty()) return@flatMap emptyList()
    val groups = image.region(zone.first, 0, zone.last + 1, image.rows()).use { findHorizontalLines(it, unitSize) }
    groups.map { group ->
        val minY = group.min()
        val maxY = group.max()
        val zoneWidth = zone.last + 1 - zone.first
        val centerX = zone.first + zoneWidth / 2.0
        RotatedBoundingBox(Box(centerX.toInt().toDouble(), ((minY + maxY) / 2.0).toInt().toDouble(), zoneWidth.toDouble(), (maxY - minY).toDouble(), 0.0), IntArray(0))
    }
}

private fun predictOtherAnchorsFromClefs(clefAnchors: List<StaffAnchor>, image: Mat): List<RotatedBoundingBox> {
    if (clefAnchors.isEmpty()) return emptyList()
    val unitSize = clefAnchors.map { it.averageUnitSize }.average()
    val symbols = clefAnchors.map { it.symbol }
    return findFiveLineGroups(image, clefZones(clefAnchors, image.cols()), unitSize).filterNot { it.isOverlappingWithAny(symbols) }
}

private fun findStaffsWithoutClef(rawStaffs: List<RawStaff>, clefAnchors: List<StaffAnchor>, fragments: List<RotatedBoundingBox>, image: Mat): List<RawStaff> {
    if (rawStaffs.isEmpty() || clefAnchors.isEmpty()) return emptyList()
    val unitSize = clefAnchors.map { it.averageUnitSize }.average()
    val width = image.cols()
    val sliceWidth = max(10, (2 * unitSize).toInt())
    val zones = SLICES_ACROSS_PAGE.map { (it * width).toInt() until min(width, (it * width).toInt() + sliceWidth) }
    val boxes = findFiveLineGroups(image, zones, unitSize)
    val anchors = filterUnusualAnchors(findStaffAnchors(fragments, boxes, areClefs = true))
    val candidates = removeDuplicateStaffs(findRawStaffs(anchors, fragments))
    val column = mainColumn(rawStaffs.map { it.minX to it.maxX })
    val minWidth = (column.second - column.first) / 2
    return candidates.filter { candidate ->
        candidate.maxX - candidate.minX >= minWidth && shareInColumn(candidate.minX, candidate.maxX, column) >= 0.9 && rawStaffs.none { candidate.isOverlapping(it) }
    }
}

internal fun breakWideFragments(fragments: List<RotatedBoundingBox>): List<RotatedBoundingBox> {
    val result = mutableListOf<RotatedBoundingBox>()
    for (fragment in fragments) {
        var remaining = fragment
        while (remaining.width > WIDE_FRAGMENT_LIMIT) {
            val points = remaining.contours.asList().chunked(2)
            val minX = points.minOf { it[0] }
            val left = points.filter { it[0] < minX + WIDE_FRAGMENT_LIMIT }.sortedBy { it[0] }.toMutableList()
            val right = points.filter { it[0] >= minX + WIDE_FRAGMENT_LIMIT }.sortedBy { it[0] }.toMutableList()
            if (left.isEmpty() || right.isEmpty()) break
            left += right.first()
            right += left.last()
            result += createRotatedBoundingBox(left.flatten().toIntArray(), remaining.debugId)
            remaining = createRotatedBoundingBox(right.flatten().toIntArray(), remaining.debugId)
        }
        result += remaining
    }
    return result
}

internal fun estimateUnitSizeFromClefs(fragments: List<RotatedBoundingBox>, clefsKeys: List<RotatedBoundingBox>): Double? {
    val anchors = findStaffAnchors(fragments, clefsKeys, areClefs = true)
    return if (anchors.isEmpty()) null else anchors.map { it.averageUnitSize }.median()
}

internal fun detectStaffs(
    image: Mat,
    fragments: List<RotatedBoundingBox>,
    clefsKeys: List<RotatedBoundingBox>,
    barLines: List<RotatedBoundingBox>,
    symbols: Mat,
): List<Staff> {
    val clefAnchors = findStaffAnchors(fragments, clefsKeys, areClefs = true)
    val possibleOtherClefs = predictOtherAnchorsFromClefs(clefAnchors, image)
    val anchors = filterUnusualAnchors(
        clefAnchors + findStaffAnchors(fragments, possibleOtherClefs, areClefs = true) + findStaffAnchors(fragments, barLines, areClefs = false),
    )
    logger.debug("Found {} clefs, {} possible other clefs and {} staff anchors", clefAnchors.size, possibleOtherClefs.size, anchors.size)
    val withDuplicates = findRawStaffs(anchors, fragments)
    val unique = removeDuplicateStaffs(withDuplicates)
    val rawStaffs = Mat().use { ink ->
        Core.bitwise_or(image, symbols, ink)
        mergeStaffsInSameRow(unique, ink)
    }
    val all = rawStaffs + findStaffsWithoutClef(rawStaffs, clefAnchors, fragments, image)
    val staffs = all.map(::resampleStaff)
    return filterNeighborPage(filterEdgeOfVision(staffs, image.cols(), image.rows())).sortedBy { it.minY }
}
