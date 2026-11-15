package de.citrinscore.i18n

import androidx.compose.ui.text.intl.Locale
import de.citrinscore.i18n.translations.CzechStrings
import de.citrinscore.i18n.translations.DanishStrings
import de.citrinscore.i18n.translations.DutchStrings
import de.citrinscore.i18n.translations.EnglishStrings
import de.citrinscore.i18n.translations.FinnishStrings
import de.citrinscore.i18n.translations.FrenchStrings
import de.citrinscore.i18n.translations.GermanStrings
import de.citrinscore.i18n.translations.GreekStrings
import de.citrinscore.i18n.translations.HungarianStrings
import de.citrinscore.i18n.translations.ItalianStrings
import de.citrinscore.i18n.translations.NorwegianStrings
import de.citrinscore.i18n.translations.PolishStrings
import de.citrinscore.i18n.translations.PortugueseStrings
import de.citrinscore.i18n.translations.RomanianStrings
import de.citrinscore.i18n.translations.RussianStrings
import de.citrinscore.i18n.translations.SpanishStrings
import de.citrinscore.i18n.translations.SwedishStrings
import de.citrinscore.i18n.translations.UkrainianStrings
import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(
    val tag: String,
    val nativeName: String,
    val strings: Strings,
    private val aliases: Set<String> = emptySet(),
) {
    German("de", "Deutsch", GermanStrings),
    English("en", "English", EnglishStrings),
    French("fr", "Français", FrenchStrings),
    Spanish("es", "Español", SpanishStrings),
    Italian("it", "Italiano", ItalianStrings),
    Portuguese("pt", "Português", PortugueseStrings),
    Dutch("nl", "Nederlands", DutchStrings),
    Polish("pl", "Polski", PolishStrings),
    Czech("cs", "Čeština", CzechStrings),
    Swedish("sv", "Svenska", SwedishStrings),
    Danish("da", "Dansk", DanishStrings),
    Norwegian("nb", "Norsk", NorwegianStrings, setOf("no", "nn")),
    Finnish("fi", "Suomi", FinnishStrings),
    Hungarian("hu", "Magyar", HungarianStrings),
    Romanian("ro", "Română", RomanianStrings),
    Greek("el", "Ελληνικά", GreekStrings),
    Ukrainian("uk", "Українська", UkrainianStrings),
    Russian("ru", "Русский", RussianStrings);

    private fun matches(language: String) = language == tag || language in aliases

    companion object {
        fun fromSystem(): AppLanguage = Locale.current.language.let { system -> entries.firstOrNull { it.matches(system) } } ?: English
    }
}
