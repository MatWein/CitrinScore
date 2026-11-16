package de.citrinscore.ui.importer

private val ImageExtensions = listOf("png", "jpg", "jpeg", "webp", "bmp", "svg")
private val PdfExtensions = listOf("pdf")

enum class ImportType(val extensions: List<String>) {
    Images(ImageExtensions),
    Scan(ImageExtensions + PdfExtensions),
    Pdf(PdfExtensions),
}
