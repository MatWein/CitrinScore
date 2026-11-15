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
}
