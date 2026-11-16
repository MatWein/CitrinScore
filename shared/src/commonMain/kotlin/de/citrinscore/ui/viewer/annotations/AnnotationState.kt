package de.citrinscore.ui.viewer.annotations

import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.TextNote

enum class AnnotationTool { Pen, Text, Eraser }

data class TextDraft(val page: Int, val x: Float, val y: Float, val size: Float, val original: TextNote?)

data class AnnotationState(
    val annotations: ScoreAnnotations = ScoreAnnotations(),
    val isEditing: Boolean = false,
    val tool: AnnotationTool = AnnotationTool.Pen,
    val canUndo: Boolean = false,
    val draft: TextDraft? = null,
    val saveFailed: Boolean = false,
)
