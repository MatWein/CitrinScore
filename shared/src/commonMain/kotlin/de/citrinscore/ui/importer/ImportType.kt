package de.citrinscore.ui.importer

enum class ImportType(val extensions: List<String>) {
    Images(listOf("png", "jpg", "jpeg", "webp", "bmp", "svg")),
    Pdf(listOf("pdf")),
}
