package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Rect

data class EngravingLayout(val pageWidth: Int, val pageHeight: Int) {
    companion object {
        val Print = EngravingLayout(pageWidth = 2100, pageHeight = 2970)
    }
}

data class StaffBox(val measure: Int, val staff: Int, val bounds: Rect, val space: Float, val contentLeft: Float = bounds.left)

data class NoteBox(val id: String, val measure: Int, val bounds: Rect)

class EngravedPage(val svg: String, val staves: List<StaffBox>, val notes: List<NoteBox> = emptyList())

interface MusicXmlEngraver : AutoCloseable {
    /** The number of parts of the source, including those left out when only the first part is engraved. */
    val partCount: Int

    suspend fun engrave(layout: EngravingLayout): List<EngravedPage>

    suspend fun perform(): ScorePerformance

    suspend fun writtenPitches(notes: Collection<String>): Map<String, Int>
}

expect suspend fun openMusicXmlEngraver(data: ByteArray, compressed: Boolean, firstPartOnly: Boolean = false): MusicXmlEngraver
