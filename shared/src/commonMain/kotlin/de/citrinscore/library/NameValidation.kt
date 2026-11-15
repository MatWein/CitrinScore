package de.citrinscore.library

enum class NameError { Empty, InvalidCharacters, AlreadyExists }

private val ForbiddenCharacters = Regex("""[\\/:*?"<>|\p{Cntrl}]""")
private val ReservedNames = setOf(".", "..")

fun validateName(name: String, taken: Collection<String>): NameError? {
    val trimmed = name.trim()
    return when {
        trimmed.isEmpty() -> NameError.Empty
        trimmed in ReservedNames || ForbiddenCharacters.containsMatchIn(trimmed) -> NameError.InvalidCharacters
        taken.any { it.equals(trimmed, ignoreCase = true) } -> NameError.AlreadyExists
        else -> null
    }
}
