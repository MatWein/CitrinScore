package de.citrinscore.score.musicxml

actual suspend fun openMusicXmlEngraver(data: ByteArray, compressed: Boolean): MusicXmlEngraver =
    throw UnsupportedOperationException("MusicXML engraving is not available on iOS yet")
