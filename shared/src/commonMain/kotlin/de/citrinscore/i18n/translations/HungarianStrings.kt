package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object HungarianStrings : Strings {
    override val next = "Tovább"
    override val back = "Vissza"
    override val getStarted = "Kezdjük"
    override fun stepOf(current: Int, total: Int) = "$current. lépés / $total"

    override val welcomeTitle = "Üdvözöl a ${AppInfo.NAME}"
    override val welcomeSubtitle = "Saját kottatárad – rendezetten, jegyzetekkel és lejátszható formában."

    override val featureLibraryTitle = "Importálj bármit"
    override val featureLibraryText = "Képek, PDF-ek és MusicXML egy helyen."
    override val featureNotesTitle = "Jegyzetelj"
    override val featureNotesText = "Lásd el kottáidat jegyzetekkel kézzel vagy szöveggel."
    override val featurePlaybackTitle = "Hallgasd meg"
    override val featurePlaybackText = "Játszd le a MusicXML-kottákat, és kövesd őket."

    override val languageTitle = "Válaszd ki a nyelvet"
    override val languageSubtitle = "Később módosíthatod a beállításokban."

    override val libraryLocationTitle = "Hol legyen a kottatárad?"
    override val libraryLocationSubtitle =
        "Válassz egy mappát a kottáidnak. Minden normál fájlként kerül oda, így bármikor biztonsági mentést készíthetsz róluk, vagy áthelyezheted őket."
    override val chooseFolder = "Mappa kiválasztása"
    override val changeFolder = "Mappa módosítása"
    override val folderPickerFailed = "Nem sikerült megnyitni a mappát. Próbáld újra."

    override val libraryTitle = "Kottatár"
    override val libraryEmptyTitle = "A kottatárad még üres"
    override val libraryEmptyText = "Az importált kották itt jelennek meg."

    override val cancel = "Mégse"
    override val create = "Létrehozás"

    override val newFolder = "Új mappa"
    override val importScores = "Kották importálása"
    override val newFolderTitle = "Új mappa létrehozása"
    override val folderNameLabel = "Mappa neve"
    override val nameEmpty = "Adj meg egy nevet."
    override val nameInvalid = "A név nem tartalmazhatja a következő karakterek egyikét sem: \\ / : * ? \" < > |"
    override val nameExists = "Ez a név már foglalt ebben a mappában."

    override val importTypeTitle = "Mit szeretnél importálni?"
    override val importTypeSubtitle = "Válaszd ki a kották formátumát."
    override val importTypeImages = "Képek"
    override val importTypeImagesText = "Egy vagy több kép, pl. beszkennelt oldalak (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Egyetlen PDF-dokumentum."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Digitális kották, amelyek le is játszhatók."
    override val comingSoon = "Hamarosan"

    override val importFilesTitle = "Válaszd ki a képeket"
    override val importFilesSubtitle = "Rendezd az oldalakat a helyes sorrendbe."
    override val addImages = "Képek hozzáadása"
    override val moveUp = "Feljebb"
    override val moveDown = "Lejjebb"
    override val remove = "Eltávolítás"

    override val importPreviewTitle = "Előnézet"
    override val importPreviewSubtitle = "Így fognak kinézni a kottáid."
    override val previewFailed = "Nem sikerült betölteni a képeket."

    override val importMetadataTitle = "Részletek"
    override val importMetadataSubtitle = "Adj címet a kottának, hogy később megtaláld."
    override val titleLabel = "Cím"
    override val composerLabel = "Zeneszerző"
    override val descriptionLabel = "Leírás"
    override val importAction = "Importálás"
    override val importFailed = "Nem sikerült menteni a kottát. Próbáld újra."
}
