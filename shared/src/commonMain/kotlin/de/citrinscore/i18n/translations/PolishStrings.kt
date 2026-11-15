package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object PolishStrings : Strings {
    override val next = "Dalej"
    override val back = "Wstecz"
    override val getStarted = "Zaczynamy"
    override fun stepOf(current: Int, total: Int) = "Krok $current z $total"

    override val welcomeTitle = "Witaj w ${AppInfo.NAME}"
    override val welcomeSubtitle =
        "Twoja osobista biblioteka nut – uporządkowana, z adnotacjami i gotowa do odtwarzania."

    override val featureLibraryTitle = "Importuj wszystko"
    override val featureLibraryText = "Obrazy, pliki PDF i MusicXML w jednym miejscu."
    override val featureNotesTitle = "Rób notatki"
    override val featureNotesText = "Dodawaj adnotacje do nut odręcznie lub tekstem."
    override val featurePlaybackTitle = "Słuchaj"
    override val featurePlaybackText = "Odtwarzaj nuty MusicXML i śledź je na bieżąco."

    override val languageTitle = "Wybierz język"
    override val languageSubtitle = "Możesz go później zmienić w ustawieniach."

    override val libraryLocationTitle = "Gdzie ma się znajdować twoja biblioteka?"
    override val libraryLocationSubtitle =
        "Wybierz folder na swoje nuty. Wszystko jest tam zapisywane jako zwykłe pliki, więc w każdej chwili możesz zrobić ich kopię zapasową lub je przenieść."
    override val chooseFolder = "Wybierz folder"
    override val changeFolder = "Zmień folder"
    override val folderPickerFailed = "Nie udało się otworzyć folderu. Spróbuj ponownie."

    override val libraryTitle = "Biblioteka"
    override val libraryEmptyTitle = "Twoja biblioteka jest pusta"
    override val libraryEmptyText = "Zaimportowane nuty pojawią się tutaj."

    override val cancel = "Anuluj"
    override val create = "Utwórz"

    override val newFolder = "Nowy folder"
    override val importScores = "Importuj nuty"
    override val newFolderTitle = "Utwórz nowy folder"
    override val folderNameLabel = "Nazwa folderu"
    override val nameEmpty = "Wpisz nazwę."
    override val nameInvalid = "Nazwa nie może zawierać żadnego z tych znaków: \\ / : * ? \" < > |"
    override val nameExists = "Ta nazwa jest już zajęta w tym folderze."

    override val importTypeTitle = "Co chcesz zaimportować?"
    override val importTypeSubtitle = "Wybierz format swoich nut."
    override val importTypeImages = "Obrazy"
    override val importTypeImagesText = "Jeden lub więcej obrazów, np. zeskanowane strony (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Pojedynczy dokument PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Cyfrowe nuty, które można też odtwarzać."
    override val comingSoon = "Wkrótce dostępne"

    override val importFilesTitle = "Wybierz obrazy"
    override val importFilesSubtitle = "Ułóż strony we właściwej kolejności."
    override val addImages = "Dodaj obrazy"
    override val moveUp = "Przenieś w górę"
    override val moveDown = "Przenieś w dół"
    override val remove = "Usuń"

    override val importPreviewTitle = "Podgląd"
    override val importPreviewSubtitle = "Tak będą wyglądać twoje nuty."
    override val previewFailed = "Nie udało się wczytać obrazów."

    override val importMetadataTitle = "Szczegóły"
    override val importMetadataSubtitle = "Nadaj nutom tytuł, aby łatwo je później znaleźć."
    override val titleLabel = "Tytuł"
    override val composerLabel = "Kompozytor"
    override val descriptionLabel = "Opis"
    override val importAction = "Importuj"
    override val importFailed = "Nie udało się zapisać nut. Spróbuj ponownie."
}
