package de.citrinscore.library

import de.citrinscore.score.CitScore

enum class NameError { Empty, InvalidCharacters, ReservedExtension, AlreadyExists }

private val ForbiddenCharacters = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
private val ReservedNames = setOf(".", "..")

fun validateName(name: String, taken: Collection<String>): NameError? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> NameError.Empty
        trimmed in ReservedNames || ForbiddenCharacters.containsMatchIn(trimmed) -> NameError.InvalidCharacters
        CitScore.isScoreDirectoryName(trimmed) -> NameError.ReservedExtension
        taken.any { it.equals(trimmed, ignoreCase = true) } -> NameError.AlreadyExists
        else -> null
    }
}
