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

    override val cancel = "Avbryt"
    override val create = "Skapa"

    override val newFolder = "Ny mapp"
    override val importScores = "Importera noter"
    override val newFolderTitle = "Skapa ny mapp"
    override val folderNameLabel = "Mappnamn"
    override val nameEmpty = "Ange ett namn."
    override val nameInvalid = "Namnet får inte innehålla något av dessa tecken: \\ / : * ? \" < > |"
    override val nameExists = "Namnet finns redan i den här mappen."

    override val importTypeTitle = "Vad vill du importera?"
    override val importTypeSubtitle = "Välj formatet på dina noter."
    override val importTypeImages = "Bilder"
    override val importTypeImagesText = "En eller flera bilder, t.ex. skannade sidor (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Ett enda PDF-dokument."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digitala noter som även kan spelas upp."
    override val comingSoon = "Kommer snart"

    override val importFilesTitle = "Välj dina bilder"
    override val importFilesSubtitle = "Ordna sidorna i rätt ordning."
    override val addImages = "Lägg till bilder"
    override val moveUp = "Flytta upp"
    override val moveDown = "Flytta ned"
    override val remove = "Ta bort"

    override val importPreviewTitle = "Förhandsgranskning"
    override val importPreviewSubtitle = "Så här kommer dina noter att se ut."
    override val previewFailed = "Bilderna kunde inte läsas in."

    override val importMetadataTitle = "Detaljer"
    override val importMetadataSubtitle = "Ge dina noter en titel så att du hittar dem senare."
    override val titleLabel = "Titel"
    override val composerLabel = "Kompositör"
    override val descriptionLabel = "Beskrivning"
    override val importAction = "Importera"
    override val importFailed = "Noterna kunde inte sparas. Försök igen."
}
