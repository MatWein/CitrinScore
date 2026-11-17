package de.citrinscore.settings

import de.citrinscore.i18n.AppLanguage
import de.citrinscore.library.LibrarySort
import kotlinx.serialization.Serializable

@Serializable
data class AppSettings(
    val language: AppLanguage,
    val library: LibraryReference,
    val theme: ThemeMode = ThemeMode.System,
    val librarySort: LibrarySort = LibrarySort.RecentlyUsed,
    val git: GitRemote? = null,
)
