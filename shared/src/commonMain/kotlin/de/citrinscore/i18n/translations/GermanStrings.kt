package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object GermanStrings : Strings {
    override val next = "Weiter"
    override val back = "Zurück"
    override val getStarted = "Los geht's"
    override fun stepOf(current: Int, total: Int) = "Schritt $current von $total"

    override val welcomeTitle = "Willkommen bei ${AppInfo.NAME}"
    override val welcomeSubtitle = "Deine persönliche Notenbibliothek – geordnet, kommentiert und abspielbar."

    override val featureLibraryTitle = "Alles importieren"
    override val featureLibraryText = "Bilder, PDFs und MusicXML an einem Ort."
    override val featureNotesTitle = "Notizen machen"
    override val featureNotesText = "Noten per Hand oder mit Text kommentieren."
    override val featurePlaybackTitle = "Anhören"
    override val featurePlaybackText = "MusicXML-Noten abspielen und mitlesen."

    override val languageTitle = "Wähle deine Sprache"
    override val languageSubtitle = "Du kannst sie später in den Einstellungen ändern."

    override val libraryLocationTitle = "Wo soll deine Bibliothek liegen?"
    override val libraryLocationSubtitle =
        "Wähle einen Ordner für deine Noten. Alles wird dort als normale Dateien gespeichert – so kannst du sie jederzeit sichern oder verschieben."
    override val chooseFolder = "Ordner wählen"
    override val changeFolder = "Ordner ändern"
    override val folderPickerFailed = "Der Ordner konnte nicht geöffnet werden. Bitte versuche es erneut."

    override val libraryTitle = "Bibliothek"
    override val libraryEmptyTitle = "Deine Bibliothek ist noch leer"
    override val libraryEmptyText = "Importierte Noten erscheinen hier."
}
