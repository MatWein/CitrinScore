package de.citrinscore.settings

import de.citrinscore.i18n.AppLanguage
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val language: AppLanguage,
    val library: LibraryReference,
)
