package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object DutchStrings : Strings {
    override val next = "Volgende"
    override val back = "Terug"
    override val getStarted = "Aan de slag"
    override fun stepOf(current: Int, total: Int) = "Stap $current van $total"

    override val welcomeTitle = "Welkom bij ${AppInfo.NAME}"
    override val welcomeSubtitle = "Je persoonlijke bladmuziekbibliotheek – overzichtelijk, geannoteerd en afspeelbaar."

    override val featureLibraryTitle = "Alles importeren"
    override val featureLibraryText = "Afbeeldingen, pdf's en MusicXML op één plek."
    override val featureNotesTitle = "Notities maken"
    override val featureNotesText = "Voorzie je bladmuziek van aantekeningen, met de hand of met tekst."
    override val featurePlaybackTitle = "Luisteren"
    override val featurePlaybackText = "Speel MusicXML-partituren af en lees mee."

    override val languageTitle = "Kies je taal"
    override val languageSubtitle = "Je kunt dit later wijzigen in de instellingen."

    override val libraryLocationTitle = "Waar moet je bibliotheek komen?"
    override val libraryLocationSubtitle =
        "Kies een map voor je bladmuziek. Alles wordt daar als gewone bestanden opgeslagen, zodat je ze altijd kunt back-uppen of verplaatsen."
    override val chooseFolder = "Map kiezen"
    override val changeFolder = "Map wijzigen"
    override val folderPickerFailed = "De map kon niet worden geopend. Probeer het opnieuw."

    override val libraryTitle = "Bibliotheek"
    override val libraryEmptyTitle = "Je bibliotheek is nog leeg"
    override val libraryEmptyText = "Geïmporteerde bladmuziek verschijnt hier."

    override val cancel = "Annuleren"
    override val create = "Aanmaken"

    override val newFolder = "Nieuwe map"
    override val importScores = "Bladmuziek importeren"
    override val newFolderTitle = "Nieuwe map aanmaken"
    override val folderNameLabel = "Mapnaam"
    override val nameEmpty = "Voer een naam in."
    override val nameInvalid = "De naam mag geen van deze tekens bevatten: \\ / : * ? \" < > |"
    override val nameExists = "Deze naam bestaat al in deze map."

    override val importTypeTitle = "Wat wil je importeren?"
    override val importTypeSubtitle = "Kies het formaat van je bladmuziek."
    override val importTypeImages = "Afbeeldingen"
    override val importTypeImagesText = "Eén of meer afbeeldingen, bijv. gescande pagina's (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Eén pdf-document."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digitale bladmuziek die je ook kunt afspelen."
    override val comingSoon = "Binnenkort beschikbaar"

    override val importFilesTitle = "Kies je afbeeldingen"
    override val importFilesSubtitle = "Zet de pagina's in de juiste volgorde."
    override val addImages = "Afbeeldingen toevoegen"
    override val moveUp = "Omhoog"
    override val moveDown = "Omlaag"
    override val remove = "Verwijderen"

    override val importPreviewTitle = "Voorbeeld"
    override val importPreviewSubtitle = "Zo komt je bladmuziek eruit te zien."
    override val previewFailed = "De afbeeldingen konden niet worden geladen."

    override val importMetadataTitle = "Details"
    override val importMetadataSubtitle = "Geef je bladmuziek een titel, zodat je die later terugvindt."
    override val titleLabel = "Titel"
    override val composerLabel = "Componist"
    override val descriptionLabel = "Beschrijving"
    override val importAction = "Importeren"
    override val importFailed = "De bladmuziek kon niet worden opgeslagen. Probeer het opnieuw."
}
