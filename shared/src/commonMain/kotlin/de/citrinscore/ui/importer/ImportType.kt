package de.citrinscore.ui.importer

enum class ImportType(val extensions: List<String>, val isAvailable: Boolean) {
    Images(listOf("png", "jpg", "jpeg", "webp", "bmp"), isAvailable = true),
    Pdf(listOf("pdf"), isAvailable = true),
    MusicXml(listOf("musicxml", "mxl", "xml"), isAvailable = false),
}
