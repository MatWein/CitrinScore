package de.citrinscore.library

import de.citrinscore.score.CitScore

enum class TransferMode { Move, Copy }

private val NumberedName = Regex("""^(.*) \((\d+)\)$""")

/**
 * Returns [name] if no entry in [taken] has it, ignoring case, and otherwise the first free name with an appended
 * number like `Etude (2)`. The extension of a score directory stays at the end.
 */
internal fun availableName(name: String, taken: Collection<String>): String {
    val occupied = taken.mapTo(HashSet()) { it.lowercase() }
    if (name.lowercase() !in occupied) return name
    val isScore = CitScore.isScoreDirectoryName(name)
    val title = if (isScore) CitScore.title(name) else name
    val numbered = NumberedName.matchEntire(title)
    val base = numbered?.groupValues?.get(1) ?: title
    val first = numbered?.groupValues?.get(2)?.toIntOrNull()?.plus(1) ?: 2
    return generateSequence(first) { it + 1 }
        .map { number -> "$base ($number)".let { if (isScore) CitScore.directoryName(it) else it } }
        .first { it.lowercase() !in occupied }
}
