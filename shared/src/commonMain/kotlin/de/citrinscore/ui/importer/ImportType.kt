package de.citrinscore.ui.importer

import de.citrinscore.score.ScoreContent

private val ImageExtensions = listOf("png", "jpg", "jpeg", "webp", "bmp", "svg")
private val PdfExtensions = listOf("pdf")
private val MusicXmlExtensions = listOf("musicxml", "xml", "mxl")

enum class ImportType(val extensions: List<String>, val singleFile: Boolean = false) {
    Images(ImageExtensions),
    Scan(ImageExtensions + PdfExtensions),
    Pdf(PdfExtensions, singleFile = true),
    MusicXml(MusicXmlExtensions, singleFile = true),
}

expect val ImportType.pickerExtensions: List<String>?

val ScoreContent.importType
    get() = when (this) {
        is ScoreContent.Images -> ImportType.Images
        is ScoreContent.Pdf -> ImportType.Pdf
        is ScoreContent.MusicXml -> ImportType.MusicXml
    }
