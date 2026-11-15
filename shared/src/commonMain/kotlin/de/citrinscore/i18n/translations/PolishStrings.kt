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
}
