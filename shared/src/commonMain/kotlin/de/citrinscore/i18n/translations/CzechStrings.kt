package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object CzechStrings : Strings {
    override val next = "Další"
    override val back = "Zpět"
    override val getStarted = "Začít"
    override fun stepOf(current: Int, total: Int) = "Krok $current z $total"

    override val welcomeTitle = "Vítejte v ${AppInfo.NAME}"
    override val welcomeSubtitle = "Vaše osobní knihovna not – přehledná, s poznámkami a připravená k přehrávání."

    override val featureLibraryTitle = "Importujte cokoli"
    override val featureLibraryText = "Obrázky, PDF a MusicXML na jednom místě."
    override val featureNotesTitle = "Dělejte si poznámky"
    override val featureNotesText = "Opatřete noty poznámkami ručně nebo textem."
    override val featurePlaybackTitle = "Poslouchejte"
    override val featurePlaybackText = "Přehrávejte noty MusicXML a sledujte je."

    override val languageTitle = "Vyberte jazyk"
    override val languageSubtitle = "Později ho můžete změnit v nastavení."

    override val libraryLocationTitle = "Kde má být vaše knihovna?"
    override val libraryLocationSubtitle =
        "Vyberte složku pro své noty. Vše se v ní ukládá jako běžné soubory, takže je můžete kdykoli zálohovat nebo přesunout."
    override val chooseFolder = "Vybrat složku"
    override val changeFolder = "Změnit složku"
    override val folderPickerFailed = "Složku se nepodařilo otevřít. Zkuste to prosím znovu."

    override val libraryTitle = "Knihovna"
    override val libraryEmptyTitle = "Vaše knihovna je prázdná"
    override val libraryEmptyText = "Importované noty se zobrazí zde."
}
