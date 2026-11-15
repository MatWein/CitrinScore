package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object SwedishStrings : Strings {
    override val next = "Nästa"
    override val back = "Tillbaka"
    override val getStarted = "Kom igång"
    override fun stepOf(current: Int, total: Int) = "Steg $current av $total"

    override val welcomeTitle = "Välkommen till ${AppInfo.NAME}"
    override val welcomeSubtitle = "Ditt personliga notbibliotek – organiserat, kommenterat och spelbart."

    override val featureLibraryTitle = "Importera allt"
    override val featureLibraryText = "Bilder, PDF:er och MusicXML på ett ställe."
    override val featureNotesTitle = "Gör anteckningar"
    override val featureNotesText = "Kommentera dina noter för hand eller med text."
    override val featurePlaybackTitle = "Lyssna"
    override val featurePlaybackText = "Spela upp MusicXML-noter och följ med."

    override val languageTitle = "Välj ditt språk"
    override val languageSubtitle = "Du kan ändra det senare i inställningarna."

    override val libraryLocationTitle = "Var ska ditt bibliotek ligga?"
    override val libraryLocationSubtitle =
        "Välj en mapp för dina noter. Allt sparas där som vanliga filer, så att du kan säkerhetskopiera eller flytta dem när du vill."
    override val chooseFolder = "Välj mapp"
    override val changeFolder = "Byt mapp"
    override val folderPickerFailed = "Mappen kunde inte öppnas. Försök igen."

    override val libraryTitle = "Bibliotek"
    override val libraryEmptyTitle = "Ditt bibliotek är tomt"
    override val libraryEmptyText = "Noter som du importerar visas här."
}
