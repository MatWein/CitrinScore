package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object ItalianStrings : Strings {
    override val next = "Avanti"
    override val back = "Indietro"
    override val getStarted = "Iniziamo"
    override fun stepOf(current: Int, total: Int) = "Passo $current di $total"

    override val welcomeTitle = "Ti diamo il benvenuto in ${AppInfo.NAME}"
    override val welcomeSubtitle = "La tua biblioteca di spartiti personale: ordinata, annotata e pronta da ascoltare."

    override val featureLibraryTitle = "Importa tutto"
    override val featureLibraryText = "Immagini, PDF e MusicXML in un unico posto."
    override val featureNotesTitle = "Prendi appunti"
    override val featureNotesText = "Annota i tuoi spartiti a mano o con il testo."
    override val featurePlaybackTitle = "Ascolta"
    override val featurePlaybackText = "Riproduci gli spartiti MusicXML e seguili."

    override val languageTitle = "Scegli la tua lingua"
    override val languageSubtitle = "Potrai cambiarla in seguito nelle impostazioni."

    override val libraryLocationTitle = "Dove vuoi salvare la tua biblioteca?"
    override val libraryLocationSubtitle =
        "Scegli una cartella per i tuoi spartiti. Tutto viene salvato lì come normali file, così puoi farne un backup o spostarli in qualsiasi momento."
    override val chooseFolder = "Scegli cartella"
    override val changeFolder = "Cambia cartella"
    override val folderPickerFailed = "Impossibile aprire la cartella. Riprova."

    override val libraryTitle = "Biblioteca"
    override val libraryEmptyTitle = "La tua biblioteca è vuota"
    override val libraryEmptyText = "Gli spartiti importati appariranno qui."

    override val cancel = "Annulla"
    override val create = "Crea"

    override val newFolder = "Nuova cartella"
    override val importScores = "Importa spartiti"
    override val newFolderTitle = "Crea nuova cartella"
    override val folderNameLabel = "Nome della cartella"
    override val nameEmpty = "Inserisci un nome."
    override val nameInvalid = "Il nome non può contenere nessuno di questi caratteri: \\ / : * ? \" < > |"
    override val nameExists = "Questo nome è già in uso in questa cartella."

    override val importTypeTitle = "Cosa vuoi importare?"
    override val importTypeSubtitle = "Scegli il formato dei tuoi spartiti."
    override val importTypeImages = "Immagini"
    override val importTypeImagesText = "Una o più immagini, ad es. pagine scansionate (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Un singolo documento PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Spartiti digitali che si possono anche riprodurre."
    override val comingSoon = "Presto disponibile"

    override val importFilesTitle = "Scegli le tue immagini"
    override val importFilesSubtitle = "Metti le pagine nell'ordine giusto."
    override val addImages = "Aggiungi immagini"
    override val moveUp = "Sposta su"
    override val moveDown = "Sposta giù"
    override val remove = "Rimuovi"

    override val importPreviewTitle = "Anteprima"
    override val importPreviewSubtitle = "Ecco come apparirà il tuo spartito."
    override val previewFailed = "Impossibile caricare le immagini."

    override val importMetadataTitle = "Dettagli"
    override val importMetadataSubtitle = "Dai un titolo al tuo spartito per ritrovarlo in seguito."
    override val titleLabel = "Titolo"
    override val composerLabel = "Compositore"
    override val descriptionLabel = "Descrizione"
    override val importAction = "Importa"
    override val importFailed = "Impossibile salvare lo spartito. Riprova."
}
