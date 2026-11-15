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

    override val cancel = "Annuller"
    override val create = "Opret"

    override val newFolder = "Ny mappe"
    override val importScores = "Importér noder"
    override val newFolderTitle = "Opret ny mappe"
    override val folderNameLabel = "Mappenavn"
    override val nameEmpty = "Indtast et navn."
    override val nameInvalid = "Navnet må ikke indeholde nogen af disse tegn: \\ / : * ? \" < > |"
    override val nameExists = "Navnet findes allerede i denne mappe."

    override val importTypeTitle = "Hvad vil du importere?"
    override val importTypeSubtitle = "Vælg formatet på dine noder."
    override val importTypeImages = "Billeder"
    override val importTypeImagesText = "Et eller flere billeder, f.eks. scannede sider (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Et enkelt PDF-dokument."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digitale noder, som også kan afspilles."
    override val comingSoon = "Kommer snart"

    override val importFilesTitle = "Vælg dine billeder"
    override val importFilesSubtitle = "Sæt siderne i den rigtige rækkefølge."
    override val addImages = "Tilføj billeder"
    override val moveUp = "Flyt op"
    override val moveDown = "Flyt ned"
    override val remove = "Fjern"

    override val importPreviewTitle = "Forhåndsvisning"
    override val importPreviewSubtitle = "Sådan kommer dine noder til at se ud."
    override val previewFailed = "Billederne kunne ikke indlæses."

    override val importMetadataTitle = "Detaljer"
    override val importMetadataSubtitle = "Giv dine noder en titel, så du kan finde dem igen."
    override val titleLabel = "Titel"
    override val composerLabel = "Komponist"
    override val descriptionLabel = "Beskrivelse"
    override val importAction = "Importér"
    override val importFailed = "Noderne kunne ikke gemmes. Prøv igen."
}
