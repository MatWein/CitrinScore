package de.citrinscore.omr

import kotlin.math.abs

internal class StaffPoint(val x: Double, val y: List<Double>, val angle: Double) {
    init {
        require(y.size % LINES_ON_A_STAFF == 0) { "A staff must consist of 5, 10, ... lines" }
    }

    val averageUnitSize = y.differences().average()

    fun merge(other: StaffPoint): StaffPoint {
        require(abs(x - other.x) <= 1e-3) { "Can't merge points at different positions" }
        return StaffPoint(x, (y + other.y).sorted(), (angle + other.angle) / 2)
    }

    fun transform(transformation: (Double, Double) -> Pair<Double, Double>): StaffPoint {
        val points = y.map { transformation(x, it) }
        return StaffPoint(points.map { it.first }.average(), points.map { it.second }, angle)
    }
}

internal class Staff(val grid: List<StaffPoint>) {
    val minX = grid.first().x
    val maxX = grid.last().x
    val minY = grid.minOf { it.y.min() }
    val maxY = grid.maxOf { it.y.max() }
    val averageUnitSize = grid.map { it.averageUnitSize }.median()
    var isGrandstaff = false

    fun merge(other: Staff): Staff {
        val gridA = grid.associateBy { roundHalfEven(it.x) }
        val gridB = other.grid.associateBy { roundHalfEven(it.x) }
        val positions = gridA.keys.intersect(gridB.keys).sorted()
        return Staff(positions.map { gridA.getValue(it).merge(gridB.getValue(it)) }).also { it.isGrandstaff = true }
    }

    fun split(): Pair<Staff, Staff>? {
        val points = grid.filter { it.y.size == 2 * LINES_ON_A_STAFF }
        if (points.isEmpty()) return null
        val upper = Staff(points.map { StaffPoint(it.x, it.y.take(LINES_ON_A_STAFF), it.angle) })
        val lower = Staff(points.map { StaffPoint(it.x, it.y.drop(LINES_ON_A_STAFF), it.angle) })
        return upper to lower
    }

    fun at(x: Double): StaffPoint? {
        val closest = grid.minBy { abs(it.x - x) }
        return closest.takeIf { abs(it.x - x) <= STAFF_POSITION_TOLERANCE }
    }

    fun transform(transformation: (Double, Double) -> Pair<Double, Double>) =
        Staff(grid.map { it.transform(transformation) }).also { it.isGrandstaff = isGrandstaff }

    fun lineAt(line: Int, x: Double): Double {
        val index = if (line < 0) grid.first().y.size + line else line
        return interpolate(x, DoubleArray(grid.size) { grid[it].x }, DoubleArray(grid.size) { grid[it].y[index] })
    }
}

internal class MultiStaff(staffs: List<Staff>, val certain: Boolean = false) {
    val staffs = staffs.sortedBy { it.minY }

    val layout get() = staffs.map { it.isGrandstaff }

    fun breakApart() = staffs.map { MultiStaff(listOf(it)) }
}

