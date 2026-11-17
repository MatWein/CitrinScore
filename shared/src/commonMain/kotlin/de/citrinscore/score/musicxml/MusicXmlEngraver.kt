package de.citrinscore.score.musicxml

import androidx.compose.ui.geometry.Rect

data class EngravingLayout(val pageWidth: Int, val pageHeight: Int) {
    companion object {
        val Print = EngravingLayout(pageWidth = 2100, pageHeight = 2970)
    }
}

data class StaffBox(val measure: Int, val staff: Int, val bounds: Rect, val space: Float, val contentLeft: Float = bounds.left)

class EngravedPage(val svg: String, val staves: List<StaffBox>)

interface MusicXmlEngraver : AutoCloseable {
    suspend fun engrave(layout: EngravingLayout): List<EngravedPage>
}

expect suspend fun openMusicXmlEngraver(data: ByteArray, compressed: Boolean): MusicXmlEngraver
