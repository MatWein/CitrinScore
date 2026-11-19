package de.citrinscore.omr

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal fun findPeaks(x: DoubleArray, height: Double, distance: Double, prominence: Double): List<Int> {
    if (x.size < 3) return emptyList()
    val candidates = mutableListOf<Int>()
    var i = 1
    while (i < x.size - 1) {
        if (x[i] >= x[i - 1]) {
            var j = i
            while (j < x.size - 1 && x[j] == x[j + 1]) j++
            if (j < x.size - 1 && x[j] > x[j + 1]) candidates += (i + j) / 2
            i = j + 1
        } else {
            i++
        }
    }
    val prominent = candidates.filter { x[it] >= height }.filter { peak ->
        var leftMin = x[peak]
        for (k in peak - 1 downTo 0) {
            if (x[k] > x[peak]) break
            leftMin = min(leftMin, x[k])
        }
        var rightMin = x[peak]
        for (k in peak + 1 until x.size) {
            if (x[k] > x[peak]) break
            rightMin = min(rightMin, x[k])
        }
        x[peak] - max(leftMin, rightMin) >= prominence
    }
    if (prominent.size <= 1) return prominent
    val keep = mutableListOf<Int>()
    prominent.sortedBy { x[it] }.asReversed().forEach { peak -> if (keep.all { abs(it - peak) >= distance }) keep += peak }
    return keep.sorted()
}
