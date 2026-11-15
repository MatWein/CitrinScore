package de.citrinscore.i18n

import androidx.compose.ui.text.intl.Locale
import de.citrinscore.i18n.translations.EnglishStrings
import de.citrinscore.i18n.translations.GermanStrings
import kotlinx.serialization.Serializable

@Serializable
enum class AppLanguage(val tag: String, val nativeName: String, val strings: Strings) {
    German("de", "Deutsch", GermanStrings),
    English("en", "English", EnglishStrings);

    companion object {
        fun fromSystem(): AppLanguage = entries.firstOrNull { it.tag == Locale.current.language } ?: English
    }
}
