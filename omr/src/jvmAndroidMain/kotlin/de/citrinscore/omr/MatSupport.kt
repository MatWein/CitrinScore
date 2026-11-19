package de.citrinscore.omr

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.RotatedRect
import org.opencv.imgproc.Imgproc

internal inline fun <M : Mat, T> M.use(block: (M) -> T): T = try {
    block(this)
} finally {
    release()
}

internal fun Mat.bytes(): ByteArray {
    val continuous = if (isContinuous) this else clone()
    return ByteArray(continuous.total().toInt() * continuous.channels()).also { continuous.get(0, 0, it) }
        .also { if (continuous !== this) continuous.release() }
}

internal fun Mat.floats(): FloatArray = FloatArray(total().toInt() * channels()).also { get(0, 0, it) }

internal fun byteMat(rows: Int, cols: Int, data: ByteArray, type: Int = CvType.CV_8UC1): Mat = Mat(rows, cols, type).apply { put(0, 0, data) }

internal fun floatMat(rows: Int, cols: Int, data: FloatArray): Mat = Mat(rows, cols, CvType.CV_32FC1).apply { put(0, 0, data) }

internal fun Mat.region(x0: Int, y0: Int, x1: Int, y1: Int): Mat = submat(Rect(x0, y0, x1 - x0, y1 - y0))

internal fun points2f(points: IntArray): MatOfPoint2f =
    MatOfPoint2f(*Array(points.size / 2) { Point(points[2 * it].toDouble(), points[2 * it + 1].toDouble()) })

internal fun pointsOf(contour: MatOfPoint): IntArray {
    val values = IntArray(contour.total().toInt() * 2)
    contour.get(0, 0, values)
    return values
}

internal fun minAreaRect(points: IntArray): RotatedRect = points2f(points).use { Imgproc.minAreaRect(it) }

internal fun findContours(image: Mat, mode: Int): List<IntArray> {
    val contours = mutableListOf<MatOfPoint>()
    Mat().use { hierarchy -> Imgproc.findContours(image, contours, hierarchy, mode, Imgproc.CHAIN_APPROX_SIMPLE) }
    return contours.map { contour -> contour.use { pointsOf(it) } }
}

internal fun boundingRect(points: IntArray): Rect = MatOfPoint(*Array(points.size / 2) { Point(points[2 * it].toDouble(), points[2 * it + 1].toDouble()) })
    .use { Imgproc.boundingRect(it) }
