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
}
