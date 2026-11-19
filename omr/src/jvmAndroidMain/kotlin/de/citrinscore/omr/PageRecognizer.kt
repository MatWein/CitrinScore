package de.citrinscore.omr

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.w3c.dom.Document

private const val STEM_SEARCH_THICKENING = 15
private val NOTEHEAD_MIN_SIZE = SizeLimit(4, 4)
private val STAFF_FRAGMENT_MIN_SIZE = SizeLimit(5, 1)
private val STAFF_FRAGMENT_MAX_SIZE = SizeLimit(10000, 100)
private val CLEF_MIN_SIZE = SizeLimit(20, 40)
private val CLEF_MAX_SIZE = SizeLimit(1000, 1000)
private val BAR_LINE_MIN_SIZE = SizeLimit(1, 5)
private const val BAR_LINE_KERNEL_WIDTH = 3
private const val BAR_LINE_KERNEL_HEIGHT = 5

internal class NoStaffsFoundException(message: String) : Exception(message)

internal class PageRecognizer(private val segnet: Segnet, private val relations: StaffRelations, private val transformer: Transformer) {

    fun recognize(page: Mat, inheritedTimes: List<TimeSignature?>?, onStaff: (Int, Int) -> Unit): Document {
        val preprocessed = autocrop(page).let { cropped ->
            val resized = resizeToTargetWidth(cropped)
            if (cropped !== page && resized !== cropped) cropped.release()
            applyClahe(resized).also { if (resized !== page) resized.release() }
        }
        val segmented = filterNoise(segnet.segment(preprocessed))
        val predictions = Predictions(
            segmented.preprocessed,
            segmented.notehead,
            segmented.symbols,
            makeLinesStronger(segmented.staff, 1, 2).also { segmented.staff.release() },
            segmented.clefsKeys,
            segmented.stemsRest,
        )
        return predictions.use { recognize(it, inheritedTimes, onStaff) }
    }

    private fun recognize(predictions: Predictions, inheritedTimes: List<TimeSignature?>?, onStaff: (Int, Int) -> Unit): Document {
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
        val systems = repairSystems(relations.groupStaffs(predictions, staffs))
        val voices = parseStaffs(systems, predictions.preprocessed, transformer, onStaff)
        return generateMusicXml(voices, inheritedTimes)
    }
}
