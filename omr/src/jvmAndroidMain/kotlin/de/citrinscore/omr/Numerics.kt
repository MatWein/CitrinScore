package de.citrinscore.omr

import kotlin.math.sqrt

internal fun Iterable<Double>.median(): Double {
    val sorted = sorted()
    val middle = sorted.size / 2
    return if (sorted.size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle]) / 2
}

internal fun Iterable<Double>.standardDeviation(): Double {
    val values = toList()
    val mean = values.average()
    return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
}

internal fun roundHalfEven(value: Double): Int = Math.rint(value).toInt()

internal fun List<Double>.differences(): List<Double> = zipWithNext { a, b -> b - a }

internal fun interpolate(x: Double, xs: DoubleArray, ys: DoubleArray): Double {
    if (x <= xs.first()) return ys.first()
    if (x >= xs.last()) return ys.last()
    val upper = xs.indexOfFirst { it > x }
    val lower = upper - 1
    val share = (x - xs[lower]) / (xs[upper] - xs[lower])
    return ys[lower] + share * (ys[upper] - ys[lower])
}

internal fun pythonRange(start: Int, stop: Int, step: Int): List<Int> =
    if (start >= stop) emptyList() else (start until stop step step).toList()
