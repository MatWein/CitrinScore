package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object DanishStrings : Strings {
    override val next = "Næste"
    override val back = "Tilbage"
    override val getStarted = "Kom i gang"
    override fun stepOf(current: Int, total: Int) = "Trin $current af $total"

    override val welcomeTitle = "Velkommen til ${AppInfo.NAME}"
    override val welcomeSubtitle = "Dit personlige nodebibliotek – organiseret, kommenteret og klar til afspilning."

    override val featureLibraryTitle = "Importér alt"
    override val featureLibraryText = "Billeder, PDF'er og MusicXML samlet ét sted."
    override val featureNotesTitle = "Tag noter"
    override val featureNotesText = "Kommentér dine noder i hånden eller med tekst."
    override val featurePlaybackTitle = "Lyt"
    override val featurePlaybackText = "Afspil MusicXML-noder og følg med."

    override val languageTitle = "Vælg dit sprog"
    override val languageSubtitle = "Du kan ændre det senere i indstillingerne."

    override val libraryLocationTitle = "Hvor skal dit bibliotek ligge?"
    override val libraryLocationSubtitle =
        "Vælg en mappe til dine noder. Alt gemmes der som almindelige filer, så du altid kan tage backup af dem eller flytte dem."
    override val chooseFolder = "Vælg mappe"
    override val changeFolder = "Skift mappe"
    override val folderPickerFailed = "Mappen kunne ikke åbnes. Prøv igen."

    override val libraryTitle = "Bibliotek"
    override val libraryEmptyTitle = "Dit bibliotek er tomt"
    override val libraryEmptyText = "Noder, du importerer, vises her."
}
