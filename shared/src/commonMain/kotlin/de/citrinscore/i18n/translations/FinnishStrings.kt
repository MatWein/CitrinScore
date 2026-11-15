package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object FinnishStrings : Strings {
    override val next = "Seuraava"
    override val back = "Takaisin"
    override val getStarted = "Aloitetaan"
    override fun stepOf(current: Int, total: Int) = "Vaihe $current/$total"

    override val welcomeTitle = "Tervetuloa ${AppInfo.NAME}-sovellukseen"
    override val welcomeSubtitle = "Oma nuottikirjastosi – järjestyksessä, merkinnöillä varustettuna ja kuunneltavissa."

    override val featureLibraryTitle = "Tuo kaikki"
    override val featureLibraryText = "Kuvat, PDF:t ja MusicXML samassa paikassa."
    override val featureNotesTitle = "Tee muistiinpanoja"
    override val featureNotesText = "Lisää nuotteihin merkintöjä käsin tai tekstinä."
    override val featurePlaybackTitle = "Kuuntele"
    override val featurePlaybackText = "Toista MusicXML-nuotteja ja seuraa mukana."

    override val languageTitle = "Valitse kieli"
    override val languageSubtitle = "Voit vaihtaa sen myöhemmin asetuksista."

    override val libraryLocationTitle = "Mihin kirjastosi tallennetaan?"
    override val libraryLocationSubtitle =
        "Valitse kansio nuoteillesi. Kaikki tallennetaan sinne tavallisina tiedostoina, joten voit varmuuskopioida tai siirtää ne milloin tahansa."
    override val chooseFolder = "Valitse kansio"
    override val changeFolder = "Vaihda kansio"
    override val folderPickerFailed = "Kansiota ei voitu avata. Yritä uudelleen."

    override val libraryTitle = "Kirjasto"
    override val libraryEmptyTitle = "Kirjastosi on tyhjä"
    override val libraryEmptyText = "Tuomasi nuotit näkyvät täällä."
}
