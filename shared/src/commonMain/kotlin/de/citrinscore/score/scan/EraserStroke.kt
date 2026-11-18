package de.citrinscore.score.scan

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path

data class EraserStroke(val points: List<Offset>, val width: Float) {
    fun path(scale: Float = 1f) = Path().apply {
        val first = points.first() * scale
        moveTo(first.x, first.y)
        points.forEach { point -> (point * scale).let { lineTo(it.x, it.y) } }
    }
}
