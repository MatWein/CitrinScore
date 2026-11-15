package de.citrinscore.i18n

import androidx.compose.runtime.staticCompositionLocalOf

interface Strings {
    val next: String
    val back: String
    val getStarted: String
    fun stepOf(current: Int, total: Int): String

    val welcomeTitle: String
    val welcomeSubtitle: String
    val featureLibraryTitle: String
    val featureLibraryText: String
    val featureNotesTitle: String
    val featureNotesText: String
    val featurePlaybackTitle: String
    val featurePlaybackText: String

    val languageTitle: String
    val languageSubtitle: String

    val libraryLocationTitle: String
    val libraryLocationSubtitle: String
    val chooseFolder: String
    val changeFolder: String
    val folderPickerFailed: String

    val libraryTitle: String
    val libraryEmptyTitle: String
    val libraryEmptyText: String
}

val LocalStrings = staticCompositionLocalOf { AppLanguage.English.strings }
