package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import java.text.Normalizer

/** An item in [folder], which is the searched folder or one of the subfolders in [path] leading down from it. */
data class SearchEntry(val item: LibraryItem, val folder: PlatformFile, val path: List<PlatformFile> = emptyList()) {
    val location get() = path.map { it.name }
}

/** A text with the character ranges that matched the query. */
data class Highlighted(val text: String, val matches: List<IntRange> = emptyList())

data class SearchHit(
    val entry: SearchEntry,
    val title: Highlighted,
    /** The composer or a piece of the description, when a part of the query was only found there. */
    val detail: Highlighted? = null,
) {
    val item get() = entry.item
}

/**
 * Finds the entries that contain every word of [query] in their title, composer or description, ignoring case,
 * diacritics and punctuation. Titles starting with the query come first, then matches in the title, the composer
 * and the description; equally good matches keep the folders first and the scores in [sort] order.
 */
fun List<SearchEntry>.search(query: String, sort: LibrarySort): List<SearchHit> {
    val words = query.split(Whitespace).map(::normalized).filter(String::isNotEmpty)
    if (words.isEmpty()) return emptyList()
    val ranked = mapNotNull { entry -> entry.match(words) }
    val order = compareBy<Pair<SearchHit, Int>> { it.second }
        .thenBy { it.first.item !is LibraryItem.Folder }
        .thenComparator { a, b -> compareItems(a.first.item, b.first.item, sort) }
    return ranked.sortedWith(order).map { it.first }
}

private fun compareItems(a: LibraryItem, b: LibraryItem, sort: LibrarySort) =
    if (a is LibraryItem.Score && b is LibraryItem.Score) sort.scoreOrder.compare(a, b)
    else a.displayName.lowercase().compareTo(b.displayName.lowercase())

private fun SearchEntry.match(words: List<String>): Pair<SearchHit, Int>? {
    val metadata = (item as? LibraryItem.Score)?.metadata
    val fields = listOf(item.displayName, metadata?.composer.orEmpty(), metadata?.description.orEmpty()).map(::SearchText)
    val found = words.map { word -> fields.indexOfFirst { word in it.normalized } }
    if (-1 in found) return null
    val title = fields[TITLE]
    val rank = if (title.normalized.startsWith(words.first())) 0 else found.max() + 1
    val detail = listOf(COMPOSER, DESCRIPTION).firstOrNull { it in found }?.let { index ->
        val highlighted = fields[index].highlight(words)
        if (index == DESCRIPTION) highlighted.excerpt() else highlighted
    }
    return SearchHit(this, title.highlight(words), detail) to rank
}

/** A text with its normalized form and, for every normalized character, the index of the original one. */
private class SearchText(val text: String) {
    val normalized: String
    private val origins: IntArray

    init {
        val builder = StringBuilder()
        val indices = ArrayList<Int>()
        text.forEachIndexed { index, char ->
            normalized(char.toString()).forEach {
                builder.append(it)
                indices += index
            }
        }
        normalized = builder.toString()
        origins = indices.toIntArray()
    }

    fun highlight(words: List<String>): Highlighted {
        val ranges = words.flatMap { word ->
            generateSequence(normalized.indexOf(word).takeIf { it >= 0 }) { start ->
                normalized.indexOf(word, start + 1).takeIf { it >= 0 }
            }.map { start -> origins[start]..origins[start + word.length - 1] }.toList()
        }
        return Highlighted(text, ranges.merged())
    }
}

private fun List<IntRange>.merged(): List<IntRange> = sortedBy { it.first }.fold(mutableListOf()) { merged, range ->
    val last = merged.lastOrNull()
    if (last != null && range.first <= last.last + 1) merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
    else merged += range
    merged
}

/** Cuts a long text down to a piece around its first match, on a single line. */
private fun Highlighted.excerpt(): Highlighted {
    val line = text.replace('\n', ' ').replace('\r', ' ')
    val first = matches.firstOrNull()?.first ?: 0
    val start = if (first <= EXCERPT_LEAD) 0 else line.lastIndexOf(' ', first - EXCERPT_LEAD).let { if (it < 0) 0 else it + 1 }
    val end = (start + EXCERPT_LENGTH).coerceAtMost(line.length)
    val prefix = if (start > 0) ELLIPSIS else ""
    val suffix = if (end < line.length) ELLIPSIS else ""
    val shift = prefix.length - start
    val ranges = matches.filter { it.first >= start && it.last < end }.map { (it.first + shift)..(it.last + shift) }
    return Highlighted(prefix + line.substring(start, end) + suffix, ranges)
}

/** Only the letters and digits, in lower case and without diacritics. */
internal fun normalized(text: String): String =
    Normalizer.normalize(text.lowercase().replace("ß", "ss"), Normalizer.Form.NFD).filter(Char::isLetterOrDigit)

private val Whitespace = Regex("\\s+")
private const val TITLE = 0
private const val COMPOSER = 1
private const val DESCRIPTION = 2
private const val EXCERPT_LEAD = 20
private const val EXCERPT_LENGTH = 80
private const val ELLIPSIS = "…"
