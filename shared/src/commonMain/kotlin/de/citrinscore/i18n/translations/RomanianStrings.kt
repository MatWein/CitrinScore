package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object RomanianStrings : Strings {
    override val next = "Înainte"
    override val back = "Înapoi"
    override val getStarted = "Să începem"
    override fun stepOf(current: Int, total: Int) = "Pasul $current din $total"

    override val welcomeTitle = "Bine ai venit în ${AppInfo.NAME}"
    override val welcomeSubtitle = "Biblioteca ta personală de partituri – organizată, adnotată și gata de redare."

    override val featureLibraryTitle = "Importă orice"
    override val featureLibraryText = "Imagini, PDF-uri și MusicXML într-un singur loc."
    override val featureNotesTitle = "Ia notițe"
    override val featureNotesText = "Adnotează partiturile de mână sau cu text."
    override val featurePlaybackTitle = "Ascultă"
    override val featurePlaybackText = "Redă partituri MusicXML și urmărește-le."

    override val languageTitle = "Alege limba"
    override val languageSubtitle = "O poți schimba mai târziu din setări."

    override val libraryLocationTitle = "Unde vrei să fie biblioteca ta?"
    override val libraryLocationSubtitle =
        "Alege un dosar pentru partituri. Totul este salvat acolo ca fișiere obișnuite, așa că le poți face copii de rezervă sau le poți muta oricând."
    override val chooseFolder = "Alege dosarul"
    override val changeFolder = "Schimbă dosarul"
    override val folderPickerFailed = "Dosarul nu a putut fi deschis. Încearcă din nou."

    override val libraryTitle = "Bibliotecă"
    override val libraryEmptyTitle = "Biblioteca ta este goală"
    override val libraryEmptyText = "Partiturile importate vor apărea aici."
}
