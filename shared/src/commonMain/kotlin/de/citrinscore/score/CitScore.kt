package de.citrinscore.score

object CitScore {
    const val EXTENSION = "citscore"
    const val MANIFEST_FILE = "manifest.json"
    const val THUMBNAIL_FILE = "thumbnail.jpg"
    const val PAGES_DIRECTORY = "pages"
    const val THUMBNAIL_WIDTH = 600
    const val PAGE_ASPECT_RATIO = 210f / 297f

    private const val SUFFIX = ".$EXTENSION"

    fun directoryName(title: String) = "$title$SUFFIX"

    fun isScoreDirectoryName(name: String) = name.endsWith(SUFFIX, ignoreCase = true)

    fun title(directoryName: String) = directoryName.dropLast(SUFFIX.length)

    fun pageFileName(index: Int, extension: String) = "${(index + 1).toString().padStart(3, '0')}.${extension.lowercase()}"
}
