package de.citrinscore.score

object CitScore {
    const val EXTENSION = "citscore"
    const val MANIFEST_ENTRY = "manifest.json"

    fun fileName(title: String) = "$title.$EXTENSION"

    fun pageEntry(index: Int, extension: String) = "pages/${(index + 1).toString().padStart(3, '0')}.${extension.lowercase()}"
}
