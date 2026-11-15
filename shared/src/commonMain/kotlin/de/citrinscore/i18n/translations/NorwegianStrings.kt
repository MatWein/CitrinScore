package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object NorwegianStrings : Strings {
    override val next = "Neste"
    override val back = "Tilbake"
    override val getStarted = "Kom i gang"
    override fun stepOf(current: Int, total: Int) = "Trinn $current av $total"

    override val welcomeTitle = "Velkommen til ${AppInfo.NAME}"
    override val welcomeSubtitle = "Ditt personlige notebibliotek – organisert, kommentert og klart til avspilling."

    override val featureLibraryTitle = "Importer alt"
    override val featureLibraryText = "Bilder, PDF-er og MusicXML samlet på ett sted."
    override val featureNotesTitle = "Ta notater"
    override val featureNotesText = "Kommenter notene dine for hånd eller med tekst."
    override val featurePlaybackTitle = "Lytt"
    override val featurePlaybackText = "Spill av MusicXML-noter og følg med."

    override val languageTitle = "Velg språk"
    override val languageSubtitle = "Du kan endre det senere i innstillingene."

    override val libraryLocationTitle = "Hvor skal biblioteket ditt ligge?"
    override val libraryLocationSubtitle =
        "Velg en mappe for notene dine. Alt lagres der som vanlige filer, slik at du når som helst kan ta sikkerhetskopi av dem eller flytte dem."
    override val chooseFolder = "Velg mappe"
    override val changeFolder = "Bytt mappe"
    override val folderPickerFailed = "Mappen kunne ikke åpnes. Prøv igjen."

    override val libraryTitle = "Bibliotek"
    override val libraryEmptyTitle = "Biblioteket ditt er tomt"
    override val libraryEmptyText = "Noter du importerer, vises her."

    override val cancel = "Avbryt"
    override val create = "Opprett"

    override val newFolder = "Ny mappe"
    override val importScores = "Importer noter"
    override val newFolderTitle = "Opprett ny mappe"
    override val folderNameLabel = "Mappenavn"
    override val nameEmpty = "Skriv inn et navn."
    override val nameInvalid = "Navnet kan ikke inneholde noen av disse tegnene: \\ / : * ? \" < > |"
    override val nameExists = "Navnet finnes allerede i denne mappen."

    override val importTypeTitle = "Hva vil du importere?"
    override val importTypeSubtitle = "Velg formatet på notene dine."
    override val importTypeImages = "Bilder"
    override val importTypeImagesText = "Ett eller flere bilder, f.eks. skannede sider (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Ett enkelt PDF-dokument."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digitale noter som også kan spilles av."
    override val comingSoon = "Kommer snart"

    override val importFilesTitle = "Velg bildene dine"
    override val importFilesSubtitle = "Sett sidene i riktig rekkefølge."
    override val addImages = "Legg til bilder"
    override val moveUp = "Flytt opp"
    override val moveDown = "Flytt ned"
    override val remove = "Fjern"

    override val importPreviewTitle = "Forhåndsvisning"
    override val importPreviewSubtitle = "Slik kommer notene dine til å se ut."
    override val previewFailed = "Bildene kunne ikke lastes inn."

    override val importMetadataTitle = "Detaljer"
    override val importMetadataSubtitle = "Gi notene en tittel, så finner du dem igjen senere."
    override val titleLabel = "Tittel"
    override val composerLabel = "Komponist"
    override val descriptionLabel = "Beskrivelse"
    override val importAction = "Importer"
    override val importFailed = "Notene kunne ikke lagres. Prøv igjen."
}
