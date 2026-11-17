package de.citrinscore.score

import kotlinx.serialization.Serializable

@Serializable
data class ScoreAnnotations(
    val strokes: List<InkStroke> = emptyList(),
    val texts: List<TextNote> = emptyList(),
    val measureStrokes: List<MeasureStroke> = emptyList(),
    val measureTexts: List<MeasureText> = emptyList(),
) {
    fun remapPages(newIndexByOldIndex: Map<Int, Int>) = copy(
        strokes = strokes.mapNotNull { stroke -> newIndexByOldIndex[stroke.page]?.let { stroke.copy(page = it) } },
        texts = texts.mapNotNull { text -> newIndexByOldIndex[text.page]?.let { text.copy(page = it) } },
    )
}

@Serializable
data class InkStroke(val page: Int, val points: List<Float>, val width: Float)

@Serializable
data class TextNote(val page: Int, val x: Float, val y: Float, val text: String, val size: Float)

@Serializable
data class MeasureAnchor(val measure: Int, val staff: Int)

@Serializable
data class MeasureStroke(val anchor: MeasureAnchor, val points: List<Float>, val width: Float)

@Serializable
data class MeasureText(val anchor: MeasureAnchor, val x: Float, val y: Float, val text: String, val size: Float)
