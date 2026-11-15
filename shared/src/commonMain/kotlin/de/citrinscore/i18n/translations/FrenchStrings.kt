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

    override val cancel = "Annuler"
    override val create = "Créer"

    override val newFolder = "Nouveau dossier"
    override val importScores = "Importer des partitions"
    override val newFolderTitle = "Créer un nouveau dossier"
    override val folderNameLabel = "Nom du dossier"
    override val nameEmpty = "Veuillez saisir un nom."
    override val nameInvalid = "Le nom ne doit contenir aucun de ces caractères : \\ / : * ? \" < > |"
    override val nameExists = "Ce nom est déjà utilisé dans ce dossier."

    override val importTypeTitle = "Que souhaitez-vous importer ?"
    override val importTypeSubtitle = "Choisissez le format de vos partitions."
    override val importTypeImages = "Images"
    override val importTypeImagesText = "Une ou plusieurs images, p. ex. des pages numérisées (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Un seul document PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Des partitions numériques que l'on peut aussi écouter."
    override val comingSoon = "Bientôt disponible"

    override val importFilesTitle = "Choisissez vos images"
    override val importFilesSubtitle = "Placez les pages dans le bon ordre."
    override val addImages = "Ajouter des images"
    override val moveUp = "Monter"
    override val moveDown = "Descendre"
    override val remove = "Retirer"

    override val importPreviewTitle = "Aperçu"
    override val importPreviewSubtitle = "Voici à quoi ressemblera votre partition."
    override val previewFailed = "Impossible de charger les images."

    override val importMetadataTitle = "Détails"
    override val importMetadataSubtitle = "Donnez un titre à votre partition pour la retrouver facilement."
    override val titleLabel = "Titre"
    override val composerLabel = "Compositeur"
    override val descriptionLabel = "Description"
    override val importAction = "Importer"
    override val importFailed = "Impossible d'enregistrer la partition. Veuillez réessayer."
}
