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

    override val cancel = "Anulează"
    override val create = "Creează"

    override val newFolder = "Dosar nou"
    override val importScores = "Importă partituri"
    override val newFolderTitle = "Creează un dosar nou"
    override val folderNameLabel = "Numele dosarului"
    override val nameEmpty = "Introdu un nume."
    override val nameInvalid = "Numele nu poate conține niciunul dintre aceste caractere: \\ / : * ? \" < > |"
    override val nameExists = "Acest nume există deja în acest dosar."

    override val importTypeTitle = "Ce vrei să imporți?"
    override val importTypeSubtitle = "Alege formatul partiturilor."
    override val importTypeImages = "Imagini"
    override val importTypeImagesText = "Una sau mai multe imagini, de ex. pagini scanate (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Un singur document PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Partituri digitale care pot fi și redate."
    override val comingSoon = "În curând"

    override val importFilesTitle = "Alege imaginile"
    override val importFilesSubtitle = "Aranjează paginile în ordinea corectă."
    override val addImages = "Adaugă imagini"
    override val moveUp = "Mută în sus"
    override val moveDown = "Mută în jos"
    override val remove = "Elimină"

    override val importPreviewTitle = "Previzualizare"
    override val importPreviewSubtitle = "Așa vor arăta partiturile tale."
    override val previewFailed = "Imaginile nu au putut fi încărcate."

    override val importMetadataTitle = "Detalii"
    override val importMetadataSubtitle = "Dă-i partiturii un titlu ca s-o găsești mai târziu."
    override val titleLabel = "Titlu"
    override val composerLabel = "Compozitor"
    override val descriptionLabel = "Descriere"
    override val importAction = "Importă"
    override val importFailed = "Partitura nu a putut fi salvată. Încearcă din nou."
}
