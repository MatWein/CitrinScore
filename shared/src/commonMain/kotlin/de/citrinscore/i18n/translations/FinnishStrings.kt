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

    override val cancel = "Peruuta"
    override val create = "Luo"

    override val newFolder = "Uusi kansio"
    override val importScores = "Tuo nuotteja"
    override val newFolderTitle = "Luo uusi kansio"
    override val folderNameLabel = "Kansion nimi"
    override val nameEmpty = "Anna nimi."
    override val nameInvalid = "Nimessä ei saa olla mitään näistä merkeistä: \\ / : * ? \" < > |"
    override val nameExists = "Tämä nimi on jo käytössä tässä kansiossa."

    override val importTypeTitle = "Mitä haluat tuoda?"
    override val importTypeSubtitle = "Valitse nuottiesi muoto."
    override val importTypeImages = "Kuvat"
    override val importTypeImagesText = "Yksi tai useampi kuva, esim. skannatut sivut (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Yksittäinen PDF-asiakirja."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digitaaliset nuotit, joita voi myös toistaa."
    override val comingSoon = "Tulossa pian"

    override val importFilesTitle = "Valitse kuvat"
    override val importFilesSubtitle = "Järjestä sivut oikeaan järjestykseen."
    override val addImages = "Lisää kuvia"
    override val moveUp = "Siirrä ylös"
    override val moveDown = "Siirrä alas"
    override val remove = "Poista"

    override val importPreviewTitle = "Esikatselu"
    override val importPreviewSubtitle = "Tältä nuottisi näyttävät."
    override val previewFailed = "Kuvia ei voitu ladata."

    override val importMetadataTitle = "Tiedot"
    override val importMetadataSubtitle = "Anna nuoteillesi nimi, jotta löydät ne myöhemmin."
    override val titleLabel = "Nimi"
    override val composerLabel = "Säveltäjä"
    override val descriptionLabel = "Kuvaus"
    override val importAction = "Tuo"
    override val importFailed = "Nuotteja ei voitu tallentaa. Yritä uudelleen."
}
