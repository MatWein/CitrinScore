package de.citrinscore.score.scan

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

private const val STRIP_COUNT = 40
private const val LINE_COVERAGE = 0.6f
private const val MAX_LINE_THICKNESS_FRACTION = 0.005f
private const val MAX_STEP_FRACTION = 0.0025f
private const val MAX_STRIP_GAP = 3
private const val MIN_TRACK_COVERAGE = 0.3f
private const val MIN_TRACKS = 5
private const val SMOOTHING_RADIUS = 1
private const val SLOPE_GRID = 8
private const val WHITE = 255

internal class StaffDewarping(private val width: Int, private val height: Int, private val ink: BooleanArray) {

    private val stripWidth = width / STRIP_COUNT

    fun apply(luminance: ByteArray): ByteArray? {
        val tracks = tracks().filter { it.size >= STRIP_COUNT * MIN_TRACK_COVERAGE }
        if (tracks.size < MIN_TRACKS || stripWidth == 0) return null
        val slopes = tracks.map(::smoothed).flatMap(::slopes)
        if (slopes.isEmpty()) return null
        return remap(luminance, displacementColumns(SlopeField(slopes)))
    }

    private fun tracks(): List<Map<Int, Float>> {
        val maxStep = width * MAX_STEP_FRACTION
        val tracks = mutableListOf<MutableMap<Int, Float>>()
        for (strip in 0 until STRIP_COUNT) {
            val centers = lineCenters(strip)
            val open = tracks.filter { track -> strip - track.keys.max() <= MAX_STRIP_GAP }
            val candidates = centers.flatMap { center ->
                open.map { track -> Triple(center, track, abs(track.predict(strip) - center)) }.filter { it.third <= maxStep }
            }.sortedBy { it.third }
            val matchedCenters = mutableSetOf<Float>()
            val matchedTracks = mutableSetOf<MutableMap<Int, Float>>()
            candidates.forEach { (center, track, _) ->
                if (center !in matchedCenters && track !in matchedTracks) {
                    track[strip] = center
                    matchedCenters += center
                    matchedTracks += track
                }
            }
            centers.filterNot { it in matchedCenters }.forEach { tracks += mutableMapOf(strip to it) }
        }
        return tracks
    }

    private fun lineCenters(strip: Int): List<Float> {
        val left = strip * stripWidth
        val required = stripWidth * LINE_COVERAGE
        val maxThickness = max(1, (width * MAX_LINE_THICKNESS_FRACTION).roundToInt())
        val centers = mutableListOf<Float>()
        var start = -1
        for (y in 0..height) {
            val isLine = y < height && (left until left + stripWidth).count { ink[y * width + it] } >= required
            if (isLine && start < 0) start = y
            if (!isLine && start >= 0) {
                if (y - start <= maxThickness) centers += (start + y - 1) / 2f
                start = -1
            }
        }
        return centers
    }

    private fun Map<Int, Float>.predict(strip: Int): Float {
        val recent = keys.sortedDescending().take(2)
        val last = getValue(recent.first())
        if (recent.size < 2) return last
        val slope = (last - getValue(recent[1])) / (recent[0] - recent[1])
        return last + slope * (strip - recent[0])
    }

    private fun smoothed(track: Map<Int, Float>): Map<Int, Float> {
        val strips = track.keys.sorted()
        val filled = (strips.first()..strips.last()).associateWith { strip ->
            track[strip] ?: run {
                val before = strips.last { it < strip }
                val after = strips.first { it > strip }
                lerp(track.getValue(before), track.getValue(after), (strip - before).toFloat() / (after - before))
            }
        }
        return filled.mapValues { (strip, _) ->
            val radius = minOf(SMOOTHING_RADIUS, strip - strips.first(), strips.last() - strip)
            ((strip - radius)..(strip + radius)).map(filled::getValue).average().toFloat()
        }
    }

    private fun slopes(track: Map<Int, Float>) = track.keys.filter { it + 1 in track }.map { strip ->
        val start = track.getValue(strip)
        val end = track.getValue(strip + 1)
        SlopeSample(strip, (start + end) / 2, end - start)
    }

    private fun displacementColumns(field: SlopeField): List<FloatArray> {
        val center = STRIP_COUNT / 2
        val columns = arrayOfNulls<FloatArray>(STRIP_COUNT)
        columns[center] = FloatArray(height)
        for (strip in center until STRIP_COUNT - 1) {
            val previous = checkNotNull(columns[strip])
            columns[strip + 1] = FloatArray(height) { y -> previous[y] + field.slope(strip, y + previous[y]) }
        }
        for (strip in center downTo 1) {
            val next = checkNotNull(columns[strip])
            columns[strip - 1] = FloatArray(height) { y -> next[y] - field.slope(strip - 1, y + next[y]) }
        }
        return columns.map(::checkNotNull)
    }

    private inner class SlopeField(samples: List<SlopeSample>) {
        private val rows = height / SLOPE_GRID + 2
        private val grid = Array(STRIP_COUNT - 1) { strip ->
            FloatArray(rows) { row ->
                val y = row * SLOPE_GRID.toFloat()
                var weighted = 0f
                var total = 0f
                samples.forEach { sample ->
                    val dx = (strip - sample.strip) * stripWidth.toFloat()
                    val dy = y - sample.y
                    val weight = 1f / (dx * dx + dy * dy + 1f)
                    weighted += weight * sample.slope
                    total += weight
                }
                weighted / total
            }
        }

        fun slope(strip: Int, y: Float): Float {
            val position = (y / SLOPE_GRID).coerceIn(0f, rows - 1f)
            val row = floor(position).toInt()
            val next = minOf(row + 1, rows - 1)
            return lerp(grid[strip][row], grid[strip][next], position - row)
        }
    }

    private fun remap(luminance: ByteArray, columns: List<FloatArray>): ByteArray {
        val output = ByteArray(width * height)
        for (x in 0 until width) {
            val position = ((x - stripWidth / 2f) / stripWidth).coerceIn(0f, STRIP_COUNT - 1f)
            val left = floor(position).toInt()
            val right = minOf(left + 1, STRIP_COUNT - 1)
            val fraction = position - left
            for (y in 0 until height) {
                val source = y + lerp(columns[left][y], columns[right][y], fraction)
                output[y * width + x] = sample(luminance, x, source).toByte()
            }
        }
        return output
    }

    private fun sample(luminance: ByteArray, x: Int, y: Float): Int {
        val top = floor(y).toInt()
        val fraction = y - top
        return lerp(value(luminance, x, top).toFloat(), value(luminance, x, top + 1).toFloat(), fraction).roundToInt()
    }

    private fun value(luminance: ByteArray, x: Int, y: Int) = if (y in 0 until height) luminance[y * width + x].toInt() and 0xFF else WHITE

    private fun lerp(start: Float, end: Float, fraction: Float) = start + (end - start) * fraction

    private class SlopeSample(val strip: Int, val y: Float, val slope: Float)
}
