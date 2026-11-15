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
}
