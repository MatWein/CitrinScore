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

    override val cancel = "Zrušit"
    override val create = "Vytvořit"

    override val newFolder = "Nová složka"
    override val importScores = "Importovat noty"
    override val newFolderTitle = "Vytvořit novou složku"
    override val folderNameLabel = "Název složky"
    override val nameEmpty = "Zadejte název."
    override val nameInvalid = "Název nesmí obsahovat žádný z těchto znaků: \\ / : * ? \" < > |"
    override val nameExists = "Tento název už v této složce existuje."

    override val importTypeTitle = "Co chcete importovat?"
    override val importTypeSubtitle = "Vyberte formát svých not."
    override val importTypeImages = "Obrázky"
    override val importTypeImagesText = "Jeden nebo více obrázků, např. naskenované stránky (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Jeden dokument PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digitální noty, které lze také přehrát."
    override val comingSoon = "Již brzy"

    override val importFilesTitle = "Vyberte obrázky"
    override val importFilesSubtitle = "Seřaďte stránky ve správném pořadí."
    override val addImages = "Přidat obrázky"
    override val moveUp = "Posunout nahoru"
    override val moveDown = "Posunout dolů"
    override val remove = "Odebrat"

    override val importPreviewTitle = "Náhled"
    override val importPreviewSubtitle = "Takto budou vaše noty vypadat."
    override val previewFailed = "Obrázky se nepodařilo načíst."

    override val importMetadataTitle = "Podrobnosti"
    override val importMetadataSubtitle = "Pojmenujte své noty, abyste je později snadno našli."
    override val titleLabel = "Název"
    override val composerLabel = "Skladatel"
    override val descriptionLabel = "Popis"
    override val importAction = "Importovat"
    override val importFailed = "Noty se nepodařilo uložit. Zkuste to prosím znovu."
}
