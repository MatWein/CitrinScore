package de.citrinscore.score.musicxml

data class EngravingLayout(val pageWidth: Int, val pageHeight: Int) {
    companion object {
        val Print = EngravingLayout(pageWidth = 2100, pageHeight = 2970)
    }
}

interface MusicXmlEngraver : AutoCloseable {
    suspend fun engrave(layout: EngravingLayout): List<String>
}

expect suspend fun openMusicXmlEngraver(data: ByteArray, compressed: Boolean): MusicXmlEngraver
