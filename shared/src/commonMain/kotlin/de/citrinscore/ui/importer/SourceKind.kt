package de.citrinscore.ui.importer

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.extension

enum class SourceKind(val extensions: List<String>) {
    Image(listOf("png", "jpg", "jpeg", "webp", "bmp", "svg")),
    Pdf(listOf("pdf")),
    MusicXml(listOf("musicxml", "xml", "mxl")),
    ;

    companion object {
        fun of(file: PlatformFile) = entries.firstOrNull { file.extension.lowercase() in it.extensions }
    }
}
