package de.citrinscore.i18n

import androidx.compose.ui.text.intl.Locale
import de.citrinscore.i18n.translations.EnglishStrings
import de.citrinscore.i18n.translations.CzechStrings
import de.citrinscore.i18n.translations.DanishStrings
import de.citrinscore.i18n.translations.DutchStrings
import de.citrinscore.i18n.translations.FinnishStrings
import de.citrinscore.i18n.translations.FrenchStrings
import de.citrinscore.i18n.translations.GermanStrings
import de.citrinscore.i18n.translations.ItalianStrings
import de.citrinscore.i18n.translations.PolishStrings
import de.citrinscore.i18n.translations.PortugueseStrings
import de.citrinscore.i18n.translations.SpanishStrings
import de.citrinscore.i18n.translations.SwedishStrings
import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val tag: String, val nativeName: String, val strings: Strings) {
    German("de", "Deutsch", GermanStrings),
    English("en", "English", EnglishStrings),
    Czech("cs", "Čeština", CzechStrings),
    Danish("da", "Dansk", DanishStrings),
    Spanish("es", "Español", SpanishStrings),
    French("fr", "Français", FrenchStrings),
    Italian("it", "Italiano", ItalianStrings),
    Dutch("nl", "Nederlands", DutchStrings),
    Polish("pl", "Polski", PolishStrings),
    Portuguese("pt", "Português", PortugueseStrings),
    Finnish("fi", "Suomi", FinnishStrings),
    Swedish("sv", "Svenska", SwedishStrings);

    companion object {
        fun fromSystem(): AppLanguage = entries.firstOrNull { it.tag == Locale.current.language } ?: English
    }
}
