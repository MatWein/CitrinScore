package de.citrinscore.omr

import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfPoint
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val TARGET_WIDTH = 1920
private const val MAX_CHROMATICITY_DIFFERENCE = 0.07
private const val PAPER_THRESHOLD_OFFSET = 30
private const val CLOSE_KERNEL = 7
private const val ERODE_KERNEL = 9
private const val BLUR_KERNEL = 7
private const val EDGE_KERNEL = 5
private const val FULL_PAGE_SHARE = 0.25
private const val PAPER_SAMPLE_STEP = 10
private const val BRIGHTNESS_SHARE = 0.25
private const val CLAHE_CLIP_LIMIT = 1.0
private const val CLAHE_TILES = 8.0

private class Chromaticity(val blue: Double, val green: Double, val red: Double) {
    fun difference(other: Chromaticity) = abs(blue - other.blue) + abs(green - other.green) + abs(red - other.red)
}

private fun chromaticity(blue: Int, green: Int, red: Int): DoubleArray {
    val b = blue + 1.0
    val g = green + 1.0
    val r = red + 1.0
    val sum = b + g + r
    return doubleArrayOf(b / sum, g / sum, r / sum)
}

private fun medianChromaticity(pixels: List<Int>, bgr: ByteArray): Chromaticity {
    val channels = Array(3) { channel ->
        pixels.map { index ->
            val values = chromaticity(bgr[index].u(), bgr[index + 1].u(), bgr[index + 2].u())
            values[channel]
        }.median()
    }
    return Chromaticity(channels[0], channels[1], channels[2])
}

private fun Byte.u() = toInt() and 0xFF

internal fun autocrop(image: Mat): Mat {
    val width = image.cols()
    val height = image.rows()
    val bgr = image.bytes()
    val gray = Mat().also { Imgproc.cvtColor(image, it, Imgproc.COLOR_BGR2GRAY) }
    val histogram = IntArray(256).also { counts -> for (i in bgr.indices step 3) counts[bgr[i].u()]++ }
    val dominant = histogram.indices.maxBy { histogram[it] }
    val threshold = gray.use { Mat().also { thresh -> Imgproc.threshold(it, thresh, (dominant - PAPER_THRESHOLD_OFFSET).toDouble(), 255.0, Imgproc.THRESH_BINARY) } }
    val thresholdBytes = threshold.bytes()
    val morph = threshold.use { thresh ->
        val closed = Mat()
        Mat.ones(CLOSE_KERNEL, CLOSE_KERNEL, CvType.CV_8U).use { Imgproc.morphologyEx(thresh, closed, Imgproc.MORPH_CLOSE, it) }
        closed.use { Mat().also { eroded -> Mat.ones(ERODE_KERNEL, ERODE_KERNEL, CvType.CV_8U).use { kernel -> Imgproc.morphologyEx(closed, eroded, Imgproc.MORPH_ERODE, kernel) } } }
    }
    val contours = morph.use { findContours(it, Imgproc.RETR_EXTERNAL) }
    var largestArea = 0.0
    var sheet: IntArray? = null
    contours.forEach { contour ->
        val area = points2f(contour).use { Imgproc.contourArea(it) }
        if (area > largestArea) {
            largestArea = area
            sheet = contour
        }
    }
    val contour = sheet ?: return image
    val rect = boundingRect(contour)
    if (rect.width < width * FULL_PAGE_SHARE || rect.height < height * FULL_PAGE_SHARE) return image
    val paper = buildList {
        var counter = 0
        for (y in rect.y until rect.y + rect.height) {
            for (x in rect.x until rect.x + rect.width) {
                if (thresholdBytes[y * width + x].u() > 0) {
                    if (counter % PAPER_SAMPLE_STEP == 0) add((y * width + x) * 3)
                    counter++
                }
            }
        }
    }
    if (paper.isEmpty()) return image
    val paperChromaticity = medianChromaticity(paper, bgr)
    val margin = max(5, max(width, height) / 100)
    val x0 = max(0, rect.x - margin)
    val y0 = max(0, rect.y - margin)
    val x1 = min(width, rect.x + rect.width + margin)
    val y1 = min(height, rect.y + rect.height + margin)
    fun isPaper(left: Int, top: Int, right: Int, bottom: Int): Boolean {
        val pixels = buildList { for (y in top until bottom) for (x in left until right) add((y * width + x) * 3) }
        return pixels.isEmpty() || medianChromaticity(pixels, bgr).difference(paperChromaticity) <= MAX_CHROMATICITY_DIFFERENCE
    }
    val left = if (isPaper(0, y0, x0, y1)) 0 else x0
    val right = if (isPaper(x1, y0, width, y1)) width else x1
    val top = if (isPaper(x0, 0, x1, y0)) 0 else y0
    val bottom = if (isPaper(x0, y1, x1, height)) height else y1
    if (left == 0 && top == 0 && right == width && bottom == height) return image
    val crop = image.region(left, top, right, bottom).clone()
    val shiftedSheet = IntArray(contour.size) { if (it % 2 == 0) contour[it] - left else contour[it] - top }
    return crop.use { removeSurroundings(it, shiftedSheet, paper, bgr) }
}

private fun removeSurroundings(crop: Mat, sheet: IntArray, paper: List<Int>, bgr: ByteArray): Mat {
    val width = crop.cols()
    val height = crop.rows()
    val besideSheet = Mat(height, width, CvType.CV_8U, Scalar(1.0))
    val hull = MatOfInt().use { indices ->
        MatOfPoint(*Array(sheet.size / 2) { Point(sheet[2 * it].toDouble(), sheet[2 * it + 1].toDouble()) }).use { points ->
            Imgproc.convexHull(points, indices)
            val all = points.toArray()
            MatOfPoint(*indices.toArray().map { all[it] }.toTypedArray())
        }
    }
    hull.use { Imgproc.fillPoly(besideSheet, listOf(it), Scalar(0.0)) }
    val paperColor = DoubleArray(3) { channel -> paper.map { bgr[it + channel].u().toDouble() }.median() }
    val paperChromaticity = medianChromaticity(paper, bgr)
    val smooth = Mat().use { blurred ->
        Imgproc.blur(crop, blurred, Size(BLUR_KERNEL.toDouble(), BLUR_KERNEL.toDouble()))
        blurred.bytes()
    }
    val beside = besideSheet.use { it.bytes() }
    val brightness = paperColor.average() * BRIGHTNESS_SHARE
    val background = ByteArray(width * height) { index ->
        val b = smooth[index * 3].u()
        val g = smooth[index * 3 + 1].u()
        val r = smooth[index * 3 + 2].u()
        val values = chromaticity(b, g, r)
        val difference = abs(values[0] - paperChromaticity.blue) + abs(values[1] - paperChromaticity.green) + abs(values[2] - paperChromaticity.red)
        val bright = (b + g + r) / 3.0 > brightness
        if (beside[index].u() > 0 && bright && difference > MAX_CHROMATICITY_DIFFERENCE) 1 else 0
    }
    val dilated = byteMat(height, width, background).use { mask ->
        Mat().use { result ->
            Mat.ones(EDGE_KERNEL, EDGE_KERNEL, CvType.CV_8U).use { Imgproc.dilate(mask, result, it) }
            result.bytes()
        }
    }
    val pixels = crop.bytes()
    val fill = paperColor.map { it.toInt().toByte() }
    for (index in 0 until width * height) {
        if ((dilated[index].u() and beside[index].u()) > 0) {
            for (channel in 0 until 3) pixels[index * 3 + channel] = fill[channel]
        }
    }
    return byteMat(height, width, pixels, CvType.CV_8UC3)
}

internal fun resizeToTargetWidth(image: Mat): Mat {
    if (image.cols() == TARGET_WIDTH) return image
    val ratio = TARGET_WIDTH.toDouble() / image.cols()
    val height = roundHalfEven(image.rows() * ratio)
    val interpolation = if (ratio < 1) Imgproc.INTER_AREA else Imgproc.INTER_CUBIC
    return Mat().also { Imgproc.resize(image, it, Size(TARGET_WIDTH.toDouble(), height.toDouble()), 0.0, 0.0, interpolation) }
}

internal fun applyClahe(image: Mat): Mat = Mat().use { gray ->
    Imgproc.cvtColor(image, gray, Imgproc.COLOR_BGR2GRAY)
    Mat().also { Imgproc.createCLAHE(CLAHE_CLIP_LIMIT, Size(CLAHE_TILES, CLAHE_TILES)).apply(gray, it) }
}
