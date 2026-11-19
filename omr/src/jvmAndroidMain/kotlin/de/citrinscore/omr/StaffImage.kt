package de.citrinscore.omr

import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat6
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Subdiv2D
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

private const val Y_INTERVALS = 6
private const val X_STEP = 80
private const val POINT_MARGIN = 10
private const val MIN_POINTS_PER_LINE = 2
private const val DEWARP_FILL = 1.0
private const val SUBDIVISION_MARGIN = 10
private const val POINT_TOLERANCE = 1e-3
private const val DEGENERATE_AREA = 1e-6
private const val DARK_THRESHOLD = 97.0
private const val AVERAGE_GRAY = 127.0
private const val CROP_MARGIN_X = 10
private const val CROP_MARGIN_Y = 50
private const val REGION_UNITS_X = 2
private const val REGION_UNITS_Y = 4
private const val NO_STAFF_BELOW = 1e12

internal class StaffRegions(staffs: List<MultiStaff>) {
    private val extents = staffs.flatMap { it.staffs }.map { it.minY to it.maxY }

    fun startOfClosestStaffAbove(y: Double) = extents.filter { it.first < y }.maxOfOrNull { it.second } ?: 0.0

    fun startOfClosestStaffBelow(y: Double) = extents.filter { it.second > y }.minOfOrNull { it.first } ?: NO_STAFF_BELOW
}

internal fun canvasSize(height: Int, width: Int): Pair<Int, Int> {
    val ratio = CANVAS_HEIGHT.toDouble() / CANVAS_WIDTH
    return if (height.toDouble() / width > ratio) {
        (width.toDouble() / height * CANVAS_HEIGHT).toInt() to CANVAS_HEIGHT
    } else {
        CANVAS_WIDTH to (height.toDouble() / width * CANVAS_WIDTH).toInt()
    }
}

private fun centerOnCanvas(image: Mat, size: Pair<Int, Int>): Mat {
    val resized = Mat().also { Imgproc.resize(image, it, Size(size.first.toDouble(), size.second.toDouble())) }
    val canvas = Mat(CANVAS_HEIGHT, CANVAS_WIDTH, CvType.CV_8U, Scalar(255.0))
    val yOffset = (CANVAS_HEIGHT - resized.rows()) / 2
    resized.use { source -> canvas.submat(Rect(0, yOffset, source.cols(), source.rows())).use { source.copyTo(it) } }
    return canvas
}

private fun cropAndReturnTop(image: Mat, x1: Double, y1: Double, x2: Double, y2: Double): Pair<Mat, Pair<Int, Int>> {
    fun limitX(x: Double) = max(0, min(image.cols() - 1, roundHalfEven(x)))
    fun limitY(y: Double) = max(0, min(image.rows() - 1, roundHalfEven(y)))
    val left = limitX(min(x1, x2))
    val top = limitY(min(y1, y2))
    val right = limitX(max(x1, x2))
    val bottom = limitY(max(y1, y2))
    return image.region(left, top, right, bottom).clone() to (left to top)
}

private fun removeBlackContoursAtEdges(gray: Mat, unitSize: Double) {
    val inverted = Mat().use { thresh ->
        Imgproc.threshold(gray, thresh, DARK_THRESHOLD, 255.0, Imgproc.THRESH_BINARY)
        Mat().also { Core.bitwise_not(thresh, it) }
    }
    inverted.use { thresh ->
        val threshold = blackSpotRemovalThreshold(unitSize)
        findContours(thresh, Imgproc.RETR_TREE).forEach { contour ->
            val rect = boundingRect(contour)
            if (rect.width < threshold || rect.height < threshold) return@forEach
            val atEdge = rect.x == 0 || rect.y == 0 || rect.x + rect.width == gray.cols() || rect.y + rect.height == gray.rows()
            if (!atEdge) return@forEach
            val mean = thresh.submat(rect).use { Core.mean(it).`val`[0] }
            if (mean < AVERAGE_GRAY) return@forEach
            gray.submat(rect).use { it.setTo(Scalar(255.0)) }
        }
    }
}

private class Triangle(val source: List<Point>, val destination: List<Point>)

private fun isDegenerate(triangle: List<Point>): Boolean {
    val v1x = triangle[1].x - triangle[0].x
    val v1y = triangle[1].y - triangle[0].y
    val v2x = triangle[2].x - triangle[0].x
    val v2y = triangle[2].y - triangle[0].y
    return abs(v1x * v2y - v1y * v2x) / 2.0 < DEGENERATE_AREA
}

private fun Point.toFloat() = Point(x.toFloat().toDouble(), y.toFloat().toDouble())

private fun triangulate(source: List<Point>, destination: List<Point>): List<Triangle> {
    require(source.size >= 3) { "Need at least 3 points for triangulation" }
    val points = source.map { it.toFloat() }
    val bounds = MatOfPoint2f(*points.toTypedArray()).use { Imgproc.boundingRect(it) }
    val subdivision = Subdiv2D(Rect(bounds.x - SUBDIVISION_MARGIN, bounds.y - SUBDIVISION_MARGIN, bounds.width + 2 * SUBDIVISION_MARGIN, bounds.height + 2 * SUBDIVISION_MARGIN))
    points.forEach { subdivision.insert(it) }
    val triangles = MatOfFloat6().use { list ->
        subdivision.getTriangleList(list)
        list.toArray().toList().chunked(6)
    }
    fun indexOf(x: Float, y: Float): Int? {
        val nearest = points.indices.minBy { hypot(points[it].x - x, points[it].y - y) }
        return nearest.takeIf { hypot(points[it].x - x, points[it].y - y) < POINT_TOLERANCE }
    }
    return triangles.mapNotNull { t ->
        val indices = listOf(indexOf(t[0], t[1]), indexOf(t[2], t[3]), indexOf(t[4], t[5]))
        if (indices.any { it == null }) return@mapNotNull null
        Triangle(indices.map { points[it!!] }, indices.map { destination[it!!].toFloat() })
    }
}

private fun affineTransform(source: List<Point>, destination: List<Point>): Mat? = try {
    MatOfPoint2f(*source.toTypedArray()).use { src -> MatOfPoint2f(*destination.toTypedArray()).use { dst -> Imgproc.getAffineTransform(src, dst) } }
} catch (_: Exception) {
    null
}

private fun boundingRectOf(points: List<Point>): Rect = MatOfPoint2f(*points.toTypedArray()).use { Imgproc.boundingRect(it) }

private fun warp(image: Mat, triangles: List<Triangle>): Mat {
    val height = image.rows()
    val width = image.cols()
    val output = Mat(height, width, image.type(), Scalar(DEWARP_FILL))
    for (triangle in triangles) {
        if (isDegenerate(triangle.source) || isDegenerate(triangle.destination)) continue
        val estimated = affineTransform(triangle.source, triangle.destination) ?: continue
        estimated.release()
        val sourceRect = boundingRectOf(triangle.source)
        val destinationRect = boundingRectOf(triangle.destination)
        if (sourceRect.width <= 0 || sourceRect.height <= 0 || destinationRect.width <= 0 || destinationRect.height <= 0) continue
        val sourceCropped = triangle.source.map { Point(it.x - sourceRect.x, it.y - sourceRect.y).toFloat() }
        val destinationCropped = triangle.destination.map { Point(it.x - destinationRect.x, it.y - destinationRect.y).toFloat() }
        if (isDegenerate(sourceCropped) || isDegenerate(destinationCropped)) continue
        val matrix = affineTransform(sourceCropped, destinationCropped) ?: continue
        val x0 = max(sourceRect.x, 0)
        val y0 = max(sourceRect.y, 0)
        val x1 = min(sourceRect.x + sourceRect.width, width)
        val y1 = min(sourceRect.y + sourceRect.height, height)
        if (x1 <= x0 || y1 <= y0 || sourceRect.x < 0 || sourceRect.y < 0) {
            matrix.release()
            continue
        }
        val warped = matrix.use { m ->
            image.region(x0, y0, x1, y1).use { source ->
                Mat().also {
                    Imgproc.warpAffine(source, it, m, Size(destinationRect.width.toDouble(), destinationRect.height.toDouble()), Imgproc.INTER_LINEAR, Core.BORDER_CONSTANT, Scalar(DEWARP_FILL))
                }
            }
        }
        val mask = Mat.zeros(destinationRect.height, destinationRect.width, CvType.CV_8U)
        MatOfPoint(*destinationCropped.map { Point(it.x.toInt().toDouble(), it.y.toInt().toDouble()) }.toTypedArray()).use { Imgproc.fillConvexPoly(mask, it, Scalar(255.0)) }
        val clipX0 = max(0, destinationRect.x)
        val clipY0 = max(0, destinationRect.y)
        val clipX1 = min(width, destinationRect.x + destinationRect.width)
        val clipY1 = min(height, destinationRect.y + destinationRect.height)
        if (clipX1 > clipX0 && clipY1 > clipY0) {
            val dx = clipX0 - destinationRect.x
            val dy = clipY0 - destinationRect.y
            val region = Rect(dx, dy, clipX1 - clipX0, clipY1 - clipY0)
            warped.submat(region).use { source ->
                mask.submat(region).use { maskRegion ->
                    output.submat(Rect(clipX0, clipY0, clipX1 - clipX0, clipY1 - clipY0)).use { target -> source.copyTo(target, maskRegion) }
                }
            }
        }
        warped.release()
        mask.release()
    }
    return output
}

private fun spanAndOptimalPoints(staff: Staff, image: Mat): Pair<List<List<Point>>, List<List<Point>>> {
    val span = mutableListOf<List<Point>>()
    val optimal = mutableListOf<List<Point>>()
    val interval = image.rows() / Y_INTERVALS
    if (interval == 0) return span to optimal
    var firstOffset: Double? = null
    for (y in pythonRange(2, image.rows() - 2, interval)) {
        val linePoints = mutableListOf<Point>()
        for (x in pythonRange(2, image.cols(), X_STEP)) {
            val point = staff.at(x.toDouble()) ?: continue
            val offset = point.y[2]
            val first = firstOffset
            val delta = if (first == null || first == 0.0) {
                firstOffset = offset
                0
            } else {
                (offset - first).toInt()
            }
            val candidate = Point(x.toDouble(), (y + delta).toDouble())
            if (candidate.x >= POINT_MARGIN && candidate.x <= image.cols() - POINT_MARGIN && candidate.y >= POINT_MARGIN && candidate.y <= image.rows() - POINT_MARGIN) {
                linePoints += candidate
            }
        }
        if (linePoints.size > MIN_POINTS_PER_LINE) {
            val averageY = linePoints.sumOf { it.y } / linePoints.size
            span += linePoints
            optimal += linePoints.map { Point(it.x, averageY.toInt().toDouble()) }
        }
    }
    return span to optimal
}

private fun dewarp(image: Mat, staff: Staff): Mat {
    val triangles = try {
        val (span, optimal) = spanAndOptimalPoints(staff, image)
        val width = image.cols().toDouble()
        val height = image.rows().toDouble()
        fun complete(lines: List<List<Point>>): List<Point> {
            val withEnds = lines.map { line -> listOf(Point(0.0, line.first().y)) + line + Point(width, line.last().y) }
            return (listOf(listOf(Point(0.0, 0.0), Point(width, 0.0))) + withEnds + listOf(listOf(Point(0.0, height), Point(width, height)))).flatten()
        }
        triangulate(complete(span), complete(optimal))
    } catch (_: Exception) {
        return image.clone()
    }
    return warp(image, triangles)
}

internal fun prepareStaffImage(staff: Staff, page: Mat, regions: StaffRegions): Mat {
    val unit = staff.averageUnitSize
    val region = listOf(
        (staff.minX - REGION_UNITS_X * unit).toInt(),
        max(staff.minY - REGION_UNITS_Y * unit, regions.startOfClosestStaffAbove(staff.minY)).toInt(),
        (staff.maxX + REGION_UNITS_X * unit).toInt(),
        min(staff.maxY + REGION_UNITS_Y * unit, regions.startOfClosestStaffBelow(staff.maxY)).toInt(),
    )
    val dimensions = canvasSize(region[3] - region[1], region[2] - region[0])
    val scaling = dimensions.second.toDouble() / (region[3] - region[1])
    val scaled = Mat().also { Imgproc.resize(page, it, Size((page.cols() * scaling).toInt().toDouble(), (page.rows() * scaling).toInt().toDouble())) }
    val scaledRegion = region.map { Math.rint(it * scaling) }
    val (cropped, topLeft) = scaled.use {
        cropAndReturnTop(it, scaledRegion[0] - CROP_MARGIN_X, scaledRegion[1] - CROP_MARGIN_Y, scaledRegion[2] + CROP_MARGIN_X, scaledRegion[3] + CROP_MARGIN_Y)
    }
    val left = topLeft.first / scaling
    val top = topLeft.second / scaling
    val transformed = staff.transform { x, y -> (x - left) * scaling to (y - top) * scaling }
    val dewarped = cropped.use { dewarp(it, transformed) }
    val (staffImage, _) = dewarped.use {
        cropAndReturnTop(it, scaledRegion[0] - topLeft.first, scaledRegion[1] - topLeft.second, scaledRegion[2] - topLeft.first, scaledRegion[3] - topLeft.second)
    }
    removeBlackContoursAtEdges(staffImage, transformed.averageUnitSize)
    return staffImage.use { centerOnCanvas(it, dimensions) }
}
