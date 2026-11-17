package de.citrinscore.score.playback

private const val DEFAULT_THRESHOLD = 0.15f
private const val LOWEST_FREQUENCY = 27.5f
private const val HIGHEST_FREQUENCY = 4200f

class PitchDetector(private val sampleRate: Int, val windowSize: Int, private val threshold: Float = DEFAULT_THRESHOLD) {
    private val minLag = (sampleRate / HIGHEST_FREQUENCY).toInt().coerceAtLeast(2)
    private val maxLag = (sampleRate / LOWEST_FREQUENCY).toInt().coerceAtMost(windowSize - 1)
    private val difference = FloatArray(maxLag + 2)

    val frameSize = windowSize + maxLag + 1

    fun frequency(frame: FloatArray, offset: Int = 0): Float? {
        require(frame.size - offset >= frameSize) { "Frame needs $frameSize samples" }
        computeDifference(frame, offset)
        normalize()
        return firstDip()?.let { sampleRate / it }
    }

    private fun computeDifference(frame: FloatArray, offset: Int) {
        for (lag in 1..maxLag + 1) {
            var sum = 0f
            for (index in offset until offset + windowSize) {
                val delta = frame[index] - frame[index + lag]
                sum += delta * delta
            }
            difference[lag] = sum
        }
    }

    private fun normalize() {
        difference[0] = 1f
        var total = 0f
        for (lag in 1..maxLag + 1) {
            total += difference[lag]
            difference[lag] = if (total == 0f) 1f else difference[lag] * lag / total
        }
    }

    private fun firstDip(): Float? = (minLag..maxLag).asSequence()
        .filter { lag -> difference[lag] <= difference[lag - 1] && difference[lag] <= difference[lag + 1] }
        .map(::interpolate)
        .firstOrNull { it.value < threshold }
        ?.lag

    private fun interpolate(lag: Int): Dip {
        val previous = difference[lag - 1]
        val current = difference[lag]
        val next = difference[lag + 1]
        val curvature = previous - 2 * current + next
        if (curvature <= 0f) return Dip(lag.toFloat(), current)
        val shift = (previous - next) / (2 * curvature)
        return Dip(lag + shift, current - curvature * shift * shift / 2)
    }

    private class Dip(val lag: Float, val value: Float)
}
