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
    override val libraryEmptyText = "Scores you import will appear here."
}
