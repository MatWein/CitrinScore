package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object EnglishStrings : Strings {
    override val next = "Next"
    override val back = "Back"
    override val getStarted = "Get started"
    override fun stepOf(current: Int, total: Int) = "Step $current of $total"

    override val welcomeTitle = "Welcome to ${AppInfo.NAME}"
    override val welcomeSubtitle = "Your personal sheet music library – organized, annotated and playable."

    override val featureLibraryTitle = "Import everything"
    override val featureLibraryText = "Images, PDFs and MusicXML in one place."
    override val featureNotesTitle = "Take notes"
    override val featureNotesText = "Annotate your scores by hand or with text."
    override val featurePlaybackTitle = "Listen"
    override val featurePlaybackText = "Play MusicXML scores and follow along."

    override val languageTitle = "Choose your language"
    override val languageSubtitle = "You can change this later in the settings."

    override val libraryLocationTitle = "Where should your library live?"
    override val libraryLocationSubtitle =
        "Pick a folder for your scores. Everything is stored there as plain files, so you can back it up or move it anytime."
    override val chooseFolder = "Choose folder"
    override val changeFolder = "Change folder"
    override val folderPickerFailed = "The folder could not be opened. Please try again."

    override val libraryTitle = "Library"
    override val libraryEmptyTitle = "Your library is empty"
    override val folderEmptyTitle = "This folder is empty"
    override val libraryEmptyText = "Scores you import will appear here."

    override val cancel = "Cancel"
    override val create = "Create"

    override val newFolder = "New folder"
    override val importScores = "Import scores"
    override val newFolderTitle = "Create new folder"
    override val folderNameLabel = "Folder name"
    override val nameEmpty = "Please enter a name."
    override val nameInvalid = "The name must not contain any of these characters: \\ / : * ? \" < > |"
    override val nameReservedExtension = "The name must not end with “.citscore”."
    override val nameExists = "This name is already taken in this folder."

    override val importTypeTitle = "What would you like to import?"
    override val importTypeSubtitle = "Choose the format of your scores."
    override val importTypeImages = "Images"
    override val importTypeImagesText = "One or more images, e.g. scanned pages (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "A single PDF document."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digital sheet music that can also be played back."
    override val comingSoon = "Coming soon"

    override val importFilesTitle = "Choose your images"
    override val importFilesSubtitle = "Arrange the pages in the right order and check the preview."
    override val addImages = "Add images"
    override val moveUp = "Move up"
    override val moveDown = "Move down"
    override val remove = "Remove"

    override val previewLabel = "Preview"
    override val previewFailed = "The images could not be loaded."

    override val importMetadataTitle = "Details"
    override val importMetadataSubtitle = "Give your score a title so you can find it later."
    override val titleLabel = "Title"
    override val composerLabel = "Composer"
    override val descriptionLabel = "Description"
    override val importAction = "Import"
    override val importFailed = "The score could not be saved. Please try again."
}
