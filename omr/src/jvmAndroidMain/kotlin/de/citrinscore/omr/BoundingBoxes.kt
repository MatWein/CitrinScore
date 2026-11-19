package de.citrinscore.omr

import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.RotatedRect
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.tan

private const val MIN_ELLIPSE_POINTS = 5

internal data class Box(val centerX: Double, val centerY: Double, val width: Double, val height: Double, val angle: Double) {
    val rect get() = RotatedRect(Point(centerX, centerY), Size(width, height), angle)

    companion object {
        fun of(rect: RotatedRect) = Box(rect.center.x, rect.center.y, rect.size.width, rect.size.height, rect.angle)
    }
}

internal sealed class AngledBoundingBox(original: Box, val contours: IntArray, val debugId: Int) {
    val box: Box = normalize(original)
    val width get() = box.width
    val height get() = box.height
    val angle get() = box.angle
    val leftX get() = box.centerX - box.width / 2
    val rightX get() = box.centerX + box.width / 2

    protected abstract val polygon: IntArray

    fun isOverlapping(other: AngledBoundingBox): Boolean {
        if (!canPossiblyTouch(other)) return false
        return polygonsOverlap(polygon, other.polygon)
    }

    fun isOverlappingWithAny(others: List<AngledBoundingBox>) = others.any { isOverlapping(it) }

    protected fun canPossiblyTouch(other: AngledBoundingBox): Boolean {
        val distance = hypot(box.centerX - other.box.centerX, box.centerY - other.box.centerY)
        return distance <= max(box.width, box.height) + max(other.box.width, other.box.height)
    }

    override fun equals(other: Any?) = other is AngledBoundingBox && box == other.box

    override fun hashCode() = box.hashCode()

    override fun toString() = box.toString()

    private companion object {
        fun normalize(box: Box): Box = when {
            box.angle > 135 -> box.copy(angle = box.angle - 180)
            box.angle < -135 -> box.copy(angle = box.angle + 180)
            box.angle > 45 -> box.copy(width = box.height, height = box.width, angle = box.angle - 90)
            box.angle < -45 -> box.copy(width = box.height, height = box.width, angle = box.angle + 90)
            else -> box
        }
    }
}

internal class RotatedBoundingBox(original: Box, contours: IntArray, debugId: Int = 0) : AngledBoundingBox(original, contours, debugId) {
    override val polygon: IntArray = boxPoints(original)

    fun isIntersecting(other: RotatedBoundingBox): Boolean {
        if (!canPossiblyTouch(other)) return false
        return Mat().use { Imgproc.rotatedRectangleIntersection(box.rect, other.box.rect, it) } != Imgproc.INTERSECT_NONE
    }

    fun withSize(width: Double, height: Double) = RotatedBoundingBox(box.copy(width = width, height = height), contours, debugId)

    fun makeBoxThicker(thickness: Int) = if (thickness <= 0) this else withSize(box.width + thickness, box.height + thickness)

    fun moveHorizontallyBy(delta: Int) = RotatedBoundingBox(box.copy(centerX = box.centerX + delta), contours, debugId)

    fun makeBoxTaller(thickness: Int) = withSize(box.width, box.height + thickness)

    fun centerExtrapolated(x: Double) = (x - box.centerX) * tan(box.angle / 180 * PI) + box.centerY

    fun isOverlappingExtrapolated(other: RotatedBoundingBox, unitSize: Double): Boolean {
        val (left, right) = if (box.centerX > other.box.centerX) other to this else this to other
        val centerX = (left.box.centerX + right.box.centerX) * 0.5
        val tolerance = toleranceForStaffLineDetection(unitSize)
        val maxGap = maxLineGapSize(unitSize)
        if (centerX - left.box.centerX - floorDiv2(left.box.width) > maxGap || right.box.centerX - centerX - floorDiv2(right.box.width) > maxGap) {
            return false
        }
        return abs(left.centerExtrapolated(centerX) - right.centerExtrapolated(centerX)) <= tolerance
    }

    private fun floorDiv2(value: Double) = kotlin.math.floor(value / 2)
}

internal class BoundingEllipse(original: Box, contours: IntArray, debugId: Int = 0) : AngledBoundingBox(original, contours, debugId) {
    override val polygon: IntArray = MatOfPoint().use { points ->
        Imgproc.ellipse2Poly(
            Point(original.centerX.toInt().toDouble(), original.centerY.toInt().toDouble()),
            Size((original.width / 2).toInt().toDouble(), (original.height / 2).toInt().toDouble()),
            original.angle.toInt(),
            0,
            360,
            1,
            points,
        )
        pointsOf(points)
    }

    fun makeBoxThicker(thickness: Int) = BoundingEllipse(box.copy(width = box.width + thickness, height = box.height + thickness), contours, debugId)
}

private fun boxPoints(box: Box): IntArray = Mat().use { points ->
    Imgproc.boxPoints(box.rect, points)
    points.floats().map { it.toInt() }.toIntArray()
}

private fun polygonsOverlap(first: IntArray, second: IntArray): Boolean = points2f(first).use { a ->
    points2f(second).use { b ->
        val area = Mat().use { Imgproc.intersectConvexConvex(a, b, it, true) }
        area > 0 || anyVertexInside(first, b) || anyVertexInside(second, a)
    }
}

private fun anyVertexInside(points: IntArray, polygon: MatOfPoint2f) = (points.indices step 2).any { i ->
    Imgproc.pointPolygonTest(polygon, Point(points[i].toDouble(), points[i + 1].toDouble()), false) >= 0
}

private fun hasValidSize(rect: RotatedRect) = !rect.size.width.isNaN() && !rect.size.height.isNaN() && rect.size.width > 0 && rect.size.height > 0

internal data class SizeLimit(val width: Int, val height: Int)

internal fun createRotatedBoundingBoxes(
    image: Mat,
    skipMerging: Boolean = false,
    minSize: SizeLimit? = null,
    maxSize: SizeLimit? = null,
): List<RotatedBoundingBox> {
    val boxes = findContours(image, Imgproc.RETR_TREE).mapIndexedNotNull { index, contour ->
        val rect = minAreaRect(contour)
        if (!hasValidSize(rect)) return@mapIndexedNotNull null
        val box = RotatedBoundingBox(Box.of(rect), contour, index)
        when {
            minSize != null && (box.width < minSize.width || box.height < minSize.height) -> null
            maxSize != null && maxSize.width > 0 && box.width > maxSize.width -> null
            maxSize != null && maxSize.height > 0 && box.height > maxSize.height -> null
            else -> box
        }
    }
    if (skipMerging) return boxes
    return mergeOverlappingBoxes(boxes).map { group ->
        val contour = group.flatMap { it.contours.asList() }.toIntArray()
        RotatedBoundingBox(Box.of(minAreaRect(contour)), contour)
    }
}

internal fun createRotatedBoundingBox(contour: IntArray, debugId: Int) = RotatedBoundingBox(Box.of(minAreaRect(contour)), contour, debugId)

internal fun createBoundingEllipses(image: Mat, minSize: SizeLimit): List<BoundingEllipse> {
    val boxes = findContours(image, Imgproc.RETR_TREE).mapIndexedNotNull { index, contour ->
        if (contour.size / 2 < MIN_ELLIPSE_POINTS) return@mapIndexedNotNull null
        val rect = points2f(contour).use { Imgproc.fitEllipse(it) }
        if (!hasValidSize(rect)) return@mapIndexedNotNull null
        BoundingEllipse(Box.of(rect), contour, index).takeUnless { it.width < minSize.width || it.height < minSize.height }
    }
    return mergeOverlappingBoxes(boxes).map { group ->
        val contour = group.flatMap { it.contours.asList() }.toIntArray()
        BoundingEllipse(Box.of(minAreaRect(contour)), contour)
    }
}

private fun <T : AngledBoundingBox> mergeOverlappingBoxes(boxes: List<T>): List<List<T>> {
    val parent = IntArray(boxes.size) { it }
    val rank = IntArray(boxes.size)
    fun find(x: Int): Int {
        if (parent[x] != x) parent[x] = find(parent[x])
        return parent[x]
    }
    for (i in boxes.indices) {
        for (j in i + 1 until boxes.size) {
            if (!boxes[i].isOverlapping(boxes[j])) continue
            val rootI = find(i)
            val rootJ = find(j)
            when {
                rootI == rootJ -> Unit
                rank[rootI] > rank[rootJ] -> parent[rootJ] = rootI
                rank[rootI] < rank[rootJ] -> parent[rootI] = rootJ
                else -> {
                    parent[rootJ] = rootI
                    rank[rootI] += 1
                }
            }
        }
    }
    val groups = LinkedHashMap<Int, MutableList<T>>()
    boxes.forEachIndexed { index, box -> groups.getOrPut(find(index)) { mutableListOf() }.add(box) }
    return groups.values.toList()
}
