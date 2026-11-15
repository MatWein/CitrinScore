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
}
