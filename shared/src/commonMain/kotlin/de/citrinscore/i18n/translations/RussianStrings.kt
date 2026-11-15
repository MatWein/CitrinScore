package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object RussianStrings : Strings {
    override val next = "Далее"
    override val back = "Назад"
    override val getStarted = "Начать"
    override fun stepOf(current: Int, total: Int) = "Шаг $current из $total"

    override val welcomeTitle = "Добро пожаловать в ${AppInfo.NAME}"
    override val welcomeSubtitle =
        "Ваша личная нотная библиотека – упорядоченная, с пометками и готовая к воспроизведению."

    override val featureLibraryTitle = "Импортируйте всё"
    override val featureLibraryText = "Изображения, PDF и MusicXML в одном месте."
    override val featureNotesTitle = "Делайте заметки"
    override val featureNotesText = "Добавляйте пометки к нотам от руки или текстом."
    override val featurePlaybackTitle = "Слушайте"
    override val featurePlaybackText = "Воспроизводите ноты MusicXML и следите за ними."

    override val languageTitle = "Выберите язык"
    override val languageSubtitle = "Его можно изменить позже в настройках."

    override val libraryLocationTitle = "Где хранить вашу библиотеку?"
    override val libraryLocationSubtitle =
        "Выберите папку для своих нот. Всё сохраняется там в виде обычных файлов, поэтому вы в любой момент можете сделать их резервную копию или переместить их."
    override val chooseFolder = "Выбрать папку"
    override val changeFolder = "Изменить папку"
    override val folderPickerFailed = "Не удалось открыть папку. Попробуйте ещё раз."

    override val libraryTitle = "Библиотека"
    override val libraryEmptyTitle = "Ваша библиотека пуста"
    override val libraryEmptyText = "Импортированные ноты появятся здесь."
}
