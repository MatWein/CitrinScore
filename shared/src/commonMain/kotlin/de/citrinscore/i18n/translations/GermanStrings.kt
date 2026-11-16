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
    override val folderEmptyTitle = "Dieser Ordner ist leer"
    override val libraryEmptyText = "Importierte Noten erscheinen hier."

    override val cancel = "Abbrechen"
    override val create = "Erstellen"

    override val refresh = "Aktualisieren"
    override val moreOptions = "Weitere Optionen"
    override val delete = "Löschen"
    override val deleteTitle = "Endgültig löschen?"
    override fun deleteScoreText(name: String) = "„$name“ wird endgültig gelöscht. Das kann nicht rückgängig gemacht werden."
    override fun deleteFolderText(name: String) =
        "Der Ordner „$name“ und sein gesamter Inhalt werden endgültig gelöscht. Das kann nicht rückgängig gemacht werden."
    override val deleteFailed = "Der Eintrag konnte nicht gelöscht werden."

    override val newFolder = "Neuer Ordner"
    override val importScores = "Noten importieren"
    override val newFolderTitle = "Neuen Ordner erstellen"
    override val folderNameLabel = "Ordnername"
    override val nameEmpty = "Bitte gib einen Namen ein."
    override val nameInvalid = "Der Name darf keines dieser Zeichen enthalten: \\ / : * ? \" < > |"
    override val nameReservedExtension = "Der Name darf nicht auf „.citscore“ enden."
    override val nameExists = "Dieser Name ist in diesem Ordner bereits vergeben."

    override val importTypeTitle = "Was möchtest du importieren?"
    override val importTypeSubtitle = "Wähle das Format deiner Noten."
    override val importTypeImages = "Bilder"
    override val importTypeImagesText = "Ein oder mehrere Bilder, z. B. eingescannte Seiten (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Ein einzelnes PDF-Dokument."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digitale Noten, die sich auch abspielen lassen."
    override val comingSoon = "Bald verfügbar"

    override val importFilesTitle = "Wähle deine Bilder"
    override val importFilesSubtitle = "Bring die Seiten in die richtige Reihenfolge und prüfe die Vorschau."
    override val addImages = "Bilder hinzufügen"
    override val moveUp = "Nach oben"
    override val moveDown = "Nach unten"
    override val remove = "Entfernen"

    override val previewLabel = "Vorschau"
    override val previewFailed = "Die Bilder konnten nicht geladen werden."

    override val importMetadataTitle = "Details"
    override val importMetadataSubtitle = "Gib deinen Noten einen Titel, damit du sie später wiederfindest."
    override val titleLabel = "Titel"
    override val composerLabel = "Komponist"
    override val descriptionLabel = "Beschreibung"
    override val importAction = "Importieren"
    override val importFailed = "Die Noten konnten nicht gespeichert werden. Bitte versuche es erneut."

    override val zoomIn = "Vergrößern"
    override val zoomOut = "Verkleinern"
    override val scoreLoadFailed = "Die Noten konnten nicht geöffnet werden."
}
