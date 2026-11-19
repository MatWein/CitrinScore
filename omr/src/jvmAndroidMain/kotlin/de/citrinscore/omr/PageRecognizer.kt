package de.citrinscore.omr

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.w3c.dom.Document
import kotlin.math.abs
import kotlin.math.hypot

private const val STEM_SEARCH_THICKENING = 15
private val NOTEHEAD_MIN_SIZE = SizeLimit(4, 4)
private val STAFF_FRAGMENT_MIN_SIZE = SizeLimit(5, 1)
private val STAFF_FRAGMENT_MAX_SIZE = SizeLimit(10000, 100)
private val CLEF_MIN_SIZE = SizeLimit(20, 40)
private val CLEF_MAX_SIZE = SizeLimit(1000, 1000)
private val BAR_LINE_MIN_SIZE = SizeLimit(1, 5)
private const val BAR_LINE_KERNEL_WIDTH = 3
private const val BAR_LINE_KERNEL_HEIGHT = 5
private const val SNAP_HORIZONTAL_UNITS = 1.5
private const val SNAP_VERTICAL_UNITS = 3.0
private const val SNAP_ON_LINES_VERTICAL_UNITS = 0.6
private const val NOTE_WIDTH_UNITS = 1.3
private const val REST_HEIGHT_UNITS = 2.0

internal class NoStaffsFoundException(message: String) : Exception(message)

internal data class Bounds(val left: Double, val top: Double, val right: Double, val bottom: Double)

internal val AngledBoundingBox.bounds get() = Bounds(box.centerX - box.width / 2, box.centerY - box.height / 2, box.centerX + box.width / 2, box.centerY + box.height / 2)

internal class SystemBounds(val bounds: Bounds, val space: Double)

internal class PageResult(val document: Document, val notes: Map<String, Pair<Bounds, Int>>, val systems: List<SystemBounds>)

private class PageMapping(private val left: Int, private val top: Int, private val scaleX: Double, private val scaleY: Double) {
    val scale get() = scaleY

    fun bounds(value: Bounds) = Bounds(value.left * scaleX + left, value.top * scaleY + top, value.right * scaleX + left, value.bottom * scaleY + top)
}

internal class PageRecognizer(private val segnet: Segnet, private val relations: StaffRelations, private val transformer: Transformer) {

    fun recognize(page: Mat, inheritedTimes: List<TimeSignature?>?, idPrefix: String, onStaff: (Int, Int) -> Unit): PageResult {
        val cropped = autocrop(page)
        val resized = resizeToTargetWidth(cropped.image)
        val mapping = PageMapping(cropped.left, cropped.top, cropped.image.cols().toDouble() / resized.cols(), cropped.image.rows().toDouble() / resized.rows())
        if (cropped.image !== page && resized !== cropped.image) cropped.image.release()
        val preprocessed = applyClahe(resized).also { if (resized !== page) resized.release() }
        val segmented = filterNoise(segnet.segment(preprocessed))
        val predictions = Predictions(
            segmented.preprocessed,
            segmented.notehead,
            segmented.symbols,
            makeLinesStronger(segmented.staff, 1, 2).also { segmented.staff.release() },
            segmented.clefsKeys,
            segmented.stemsRest,
        )
        return predictions.use { recognize(it, inheritedTimes, idPrefix, mapping, onStaff) }
    }

    private fun recognize(predictions: Predictions, inheritedTimes: List<TimeSignature?>?, idPrefix: String, mapping: PageMapping, onStaff: (Int, Int) -> Unit): PageResult {
        val noteheads = createBoundingEllipses(predictions.notehead, NOTEHEAD_MIN_SIZE)
        val fragments = breakWideFragments(createRotatedBoundingBoxes(predictions.staff, skipMerging = true, minSize = STAFF_FRAGMENT_MIN_SIZE, maxSize = STAFF_FRAGMENT_MAX_SIZE))
        val clefsKeys = createRotatedBoundingBoxes(predictions.clefsKeys, minSize = CLEF_MIN_SIZE, maxSize = CLEF_MAX_SIZE)
        val stemsRest = createRotatedBoundingBoxes(predictions.stemsRest)
        val barLineCandidates = Mat.ones(BAR_LINE_KERNEL_HEIGHT, BAR_LINE_KERNEL_WIDTH, CvType.CV_8U).use { kernel ->
            Mat().use { dilated ->
                Imgproc.dilate(predictions.stemsRest, dilated, kernel)
                createRotatedBoundingBoxes(dilated, skipMerging = true, minSize = BAR_LINE_MIN_SIZE)
            }
        }
        val stems = noteheads.sortedBy { it.box.centerY }.mapNotNull { notehead ->
            val thickened = notehead.makeBoxThicker(STEM_SEARCH_THICKENING)
            stemsRest.firstOrNull { it.isOverlapping(thickened) }
        }
        val unitSize = if (noteheads.isNotEmpty()) {
            noteheads.map { it.height }.median()
        } else {
            estimateUnitSizeFromClefs(fragments, clefsKeys) ?: throw NoStaffsFoundException("No noteheads or clefs found")
        }
        val barLines = barLineCandidates
            .filter { !it.isOverlappingWithAny(noteheads) && !it.isOverlappingWithAny(stems) }
            .filter { it.height >= barLineMinHeight(unitSize) && it.width <= barLineMaxWidth(unitSize) }
        val staffs = detectStaffs(predictions.staff, fragments, clefsKeys, barLines, predictions.symbols)
        if (staffs.isEmpty()) throw NoStaffsFoundException("No staffs found")
        val parsed = parseStaffs(repairSystems(relations.groupStaffs(predictions, staffs)), predictions.preprocessed, noteheads, transformer, onStaff)
        val generated = generateMusicXml(parsed.voices, inheritedTimes, idPrefix)
        val used = generated.locations.values.mapNotNull { it.location.head }.toMutableSet()
        val notes = generated.locations.mapValues { (_, note) -> mapping.bounds(noteBounds(note, noteheads, used, unitSize)) to note.location.system }
        val systems = parsed.systems.map { system ->
            val bounds = Bounds(system.staffs.minOf { it.minX }, system.staffs.minOf { it.minY }, system.staffs.maxOf { it.maxX }, system.staffs.maxOf { it.maxY })
            SystemBounds(mapping.bounds(bounds), system.staffs.map { it.averageUnitSize }.median() * mapping.scale)
        }
        return PageResult(generated.document, notes, systems)
    }

    private fun noteBounds(note: NoteLocation, noteheads: List<BoundingEllipse>, used: MutableSet<Bounds>, unitSize: Double): Bounds {
        note.location.head?.let { return it }
        val x = note.location.x
        fun nearest(targetY: Double, vertical: Double) = noteheads.takeUnless { note.isRest }.orEmpty()
            .filter { it.bounds !in used && abs(it.box.centerX - x) <= SNAP_HORIZONTAL_UNITS * unitSize && abs(it.box.centerY - targetY) <= vertical * unitSize }
            .minByOrNull { hypot(it.box.centerX - x, it.box.centerY - targetY) }
        val snapped = note.location.lineY?.let { nearest(it, SNAP_ON_LINES_VERTICAL_UNITS) } ?: nearest(note.location.y, SNAP_VERTICAL_UNITS)
        if (snapped != null) {
            used += snapped.bounds
            return snapped.bounds
        }
        val y = note.location.lineY ?: note.location.y
        val halfWidth = (if (note.isRest) unitSize else NOTE_WIDTH_UNITS * unitSize) / 2
        val halfHeight = (if (note.isRest) REST_HEIGHT_UNITS * unitSize else unitSize) / 2
        return Bounds(x - halfWidth, y - halfHeight, x + halfWidth, y + halfHeight)
    }
}
