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

    val cancel: String
    val create: String

    val newFolder: String
    val importScores: String
    val newFolderTitle: String
    val folderNameLabel: String
    val nameEmpty: String
    val nameInvalid: String
    val nameExists: String

    val importTypeTitle: String
    val importTypeSubtitle: String
    val importTypeImages: String
    val importTypeImagesText: String
    val importTypePdf: String
    val importTypePdfText: String
    val importTypeMusicXml: String
    val importTypeMusicXmlText: String
    val comingSoon: String

    val importFilesTitle: String
    val importFilesSubtitle: String
    val addImages: String
    val moveUp: String
    val moveDown: String
    val remove: String

    val importPreviewTitle: String
    val importPreviewSubtitle: String
    val previewFailed: String

    val importMetadataTitle: String
    val importMetadataSubtitle: String
    val titleLabel: String
    val composerLabel: String
    val descriptionLabel: String
    val importAction: String
    val importFailed: String
}

val LocalStrings = staticCompositionLocalOf { AppLanguage.English.strings }
