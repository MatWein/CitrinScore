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
private const val WHITE = 255

internal class StaffDewarping(private val width: Int, private val height: Int, private val ink: BooleanArray) {

    private val stripWidth = width / STRIP_COUNT

    fun apply(luminance: ByteArray): ByteArray? {
        val tracks = tracks().filter { it.size >= STRIP_COUNT * MIN_TRACK_COVERAGE }
        if (tracks.size < MIN_TRACKS || stripWidth == 0) return null
        val columns = displacementColumns(tracks.map(::displacements))
        return remap(luminance, columns)
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

    private fun displacements(track: Map<Int, Float>): Pair<Float, Map<Int, Float>> {
        val target = track.values.average().toFloat()
        val strips = track.keys.sorted()
        val filled = (strips.first()..strips.last()).associateWith { strip ->
            track[strip] ?: run {
                val before = strips.last { it < strip }
                val after = strips.first { it > strip }
                lerp(track.getValue(before), track.getValue(after), (strip - before).toFloat() / (after - before))
            }
        }
        val smoothed = filled.mapValues { (strip, _) ->
            ((strip - SMOOTHING_RADIUS)..(strip + SMOOTHING_RADIUS)).mapNotNull { filled[it] }.average().toFloat() - target
        }
        return target to smoothed
    }

    private fun displacementColumns(tracks: List<Pair<Float, Map<Int, Float>>>): List<FloatArray> {
        val samples = (0 until STRIP_COUNT).map { strip ->
            tracks.mapNotNull { (target, displacement) -> displacement[strip]?.let { target to it } }.sortedBy { it.first }
        }
        val known = samples.indices.filter { samples[it].isNotEmpty() }
        return samples.indices.map { strip ->
            val nearest = samples[known.minBy { abs(it - strip) }]
            FloatArray(height) { y -> interpolate(nearest, y.toFloat()) }
        }
    }

    private fun interpolate(samples: List<Pair<Float, Float>>, y: Float): Float {
        val below = samples.lastOrNull { it.first <= y } ?: return samples.first().second
        val above = samples.firstOrNull { it.first > y } ?: return below.second
        return lerp(below.second, above.second, (y - below.first) / (above.first - below.first))
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
}
