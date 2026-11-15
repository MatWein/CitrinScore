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

    override val cancel = "Отмена"
    override val create = "Создать"

    override val newFolder = "Новая папка"
    override val importScores = "Импортировать ноты"
    override val newFolderTitle = "Создать новую папку"
    override val folderNameLabel = "Имя папки"
    override val nameEmpty = "Введите имя."
    override val nameInvalid = "Имя не может содержать ни один из этих символов: \\ / : * ? \" < > |"
    override val nameExists = "Это имя уже занято в этой папке."

    override val importTypeTitle = "Что вы хотите импортировать?"
    override val importTypeSubtitle = "Выберите формат своих нот."
    override val importTypeImages = "Изображения"
    override val importTypeImagesText =
        "Одно или несколько изображений, напр. отсканированные страницы (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Один документ PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Цифровые ноты, которые также можно воспроизводить."
    override val comingSoon = "Скоро"

    override val importFilesTitle = "Выберите изображения"
    override val importFilesSubtitle = "Расположите страницы в правильном порядке."
    override val addImages = "Добавить изображения"
    override val moveUp = "Переместить вверх"
    override val moveDown = "Переместить вниз"
    override val remove = "Удалить"

    override val importPreviewTitle = "Предпросмотр"
    override val importPreviewSubtitle = "Так будут выглядеть ваши ноты."
    override val previewFailed = "Не удалось загрузить изображения."

    override val importMetadataTitle = "Подробности"
    override val importMetadataSubtitle = "Дайте нотам название, чтобы потом легко их найти."
    override val titleLabel = "Название"
    override val composerLabel = "Композитор"
    override val descriptionLabel = "Описание"
    override val importAction = "Импортировать"
    override val importFailed = "Не удалось сохранить ноты. Попробуйте ещё раз."
}
