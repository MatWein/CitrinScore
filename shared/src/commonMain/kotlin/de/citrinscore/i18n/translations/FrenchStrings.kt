package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object FrenchStrings : Strings {
    override val next = "Suivant"
    override val back = "Retour"
    override val getStarted = "C'est parti"
    override fun stepOf(current: Int, total: Int) = "Étape $current sur $total"

    override val welcomeTitle = "Bienvenue dans ${AppInfo.NAME}"
    override val welcomeSubtitle = "Votre bibliothèque de partitions personnelle – organisée, annotée et prête à jouer."

    override val featureLibraryTitle = "Tout importer"
    override val featureLibraryText = "Images, PDF et MusicXML réunis au même endroit."
    override val featureNotesTitle = "Prendre des notes"
    override val featureNotesText = "Annotez vos partitions à la main ou avec du texte."
    override val featurePlaybackTitle = "Écouter"
    override val featurePlaybackText = "Lisez les partitions MusicXML et suivez la lecture."

    override val languageTitle = "Choisissez votre langue"
    override val languageSubtitle = "Vous pourrez la modifier plus tard dans les paramètres."

    override val libraryLocationTitle = "Où ranger votre bibliothèque ?"
    override val libraryLocationSubtitle =
        "Choisissez un dossier pour vos partitions. Tout y est enregistré sous forme de fichiers ordinaires, que vous pouvez sauvegarder ou déplacer à tout moment."
    override val chooseFolder = "Choisir un dossier"
    override val changeFolder = "Changer de dossier"
    override val folderPickerFailed = "Impossible d'ouvrir le dossier. Veuillez réessayer."

    override val libraryTitle = "Bibliothèque"
    override val libraryEmptyTitle = "Votre bibliothèque est vide"
    override val libraryEmptyText = "Les partitions importées apparaîtront ici."
}
