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
}
