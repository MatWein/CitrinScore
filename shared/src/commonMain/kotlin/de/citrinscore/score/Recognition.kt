package de.citrinscore.score

import kotlinx.serialization.Serializable

@Serializable
data class Recognition(val musicXml: String = CitScore.RECOGNITION_MUSICXML_FILE, val layout: String = CitScore.RECOGNITION_LAYOUT_FILE)

@Serializable
data class RecognitionLayout(val notes: List<RecognizedNote>, val measures: List<RecognizedMeasure>)

@Serializable
data class RecognizedNote(val id: String, val page: Int, val measure: Int, val left: Float, val top: Float, val right: Float, val bottom: Float)

@Serializable
data class RecognizedMeasure(val page: Int, val measure: Int, val left: Float, val top: Float, val right: Float, val bottom: Float, val space: Float)

class RecognizedMusic(val musicXml: String, val layout: RecognitionLayout)
