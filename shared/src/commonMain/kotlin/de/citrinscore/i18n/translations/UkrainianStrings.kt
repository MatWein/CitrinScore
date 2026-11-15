package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object UkrainianStrings : Strings {
    override val next = "Далі"
    override val back = "Назад"
    override val getStarted = "Почати"
    override fun stepOf(current: Int, total: Int) = "Крок $current з $total"

    override val welcomeTitle = "Ласкаво просимо до ${AppInfo.NAME}"
    override val welcomeSubtitle = "Ваша особиста нотна бібліотека – впорядкована, з нотатками й готова до відтворення."

    override val featureLibraryTitle = "Імпортуйте все"
    override val featureLibraryText = "Зображення, PDF і MusicXML в одному місці."
    override val featureNotesTitle = "Робіть нотатки"
    override val featureNotesText = "Додавайте примітки до нот від руки або текстом."
    override val featurePlaybackTitle = "Слухайте"
    override val featurePlaybackText = "Відтворюйте ноти MusicXML і стежте за ними."

    override val languageTitle = "Оберіть мову"
    override val languageSubtitle = "Її можна змінити пізніше в налаштуваннях."

    override val libraryLocationTitle = "Де зберігати вашу бібліотеку?"
    override val libraryLocationSubtitle =
        "Оберіть теку для своїх нот. Усе зберігається там у вигляді звичайних файлів, тож ви будь-коли можете створити їх резервну копію або перемістити їх."
    override val chooseFolder = "Обрати теку"
    override val changeFolder = "Змінити теку"
    override val folderPickerFailed = "Не вдалося відкрити теку. Спробуйте ще раз."

    override val libraryTitle = "Бібліотека"
    override val libraryEmptyTitle = "Ваша бібліотека порожня"
    override val libraryEmptyText = "Імпортовані ноти з’являться тут."

    override val cancel = "Скасувати"
    override val create = "Створити"

    override val newFolder = "Нова тека"
    override val importScores = "Імпортувати ноти"
    override val newFolderTitle = "Створити нову теку"
    override val folderNameLabel = "Назва теки"
    override val nameEmpty = "Введіть назву."
    override val nameInvalid = "Назва не може містити жодного з цих символів: \\ / : * ? \" < > |"
    override val nameExists = "Ця назва вже існує в цій теці."

    override val importTypeTitle = "Що ви хочете імпортувати?"
    override val importTypeSubtitle = "Оберіть формат своїх нот."
    override val importTypeImages = "Зображення"
    override val importTypeImagesText = "Одне або кілька зображень, напр. відскановані сторінки (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Один документ PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Цифрові ноти, які також можна відтворювати."
    override val comingSoon = "Незабаром"

    override val importFilesTitle = "Оберіть зображення"
    override val importFilesSubtitle = "Розташуйте сторінки в правильному порядку."
    override val addImages = "Додати зображення"
    override val moveUp = "Перемістити вгору"
    override val moveDown = "Перемістити вниз"
    override val remove = "Видалити"

    override val importPreviewTitle = "Попередній перегляд"
    override val importPreviewSubtitle = "Так виглядатимуть ваші ноти."
    override val previewFailed = "Не вдалося завантажити зображення."

    override val importMetadataTitle = "Подробиці"
    override val importMetadataSubtitle = "Дайте нотам назву, щоб потім легко їх знайти."
    override val titleLabel = "Назва"
    override val composerLabel = "Композитор"
    override val descriptionLabel = "Опис"
    override val importAction = "Імпортувати"
    override val importFailed = "Не вдалося зберегти ноти. Спробуйте ще раз."
}
