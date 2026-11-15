package de.citrinscore.i18n.translations

import de.citrinscore.AppInfo
import de.citrinscore.i18n.Strings

object SpanishStrings : Strings {
    override val next = "Siguiente"
    override val back = "Atrás"
    override val getStarted = "Empezar"
    override fun stepOf(current: Int, total: Int) = "Paso $current de $total"

    override val welcomeTitle = "Te damos la bienvenida a ${AppInfo.NAME}"
    override val welcomeSubtitle = "Tu biblioteca de partituras personal: organizada, anotada y lista para sonar."

    override val featureLibraryTitle = "Importa todo"
    override val featureLibraryText = "Imágenes, PDF y MusicXML en un solo lugar."
    override val featureNotesTitle = "Toma notas"
    override val featureNotesText = "Anota tus partituras a mano o con texto."
    override val featurePlaybackTitle = "Escucha"
    override val featurePlaybackText = "Reproduce partituras MusicXML y síguelas."

    override val languageTitle = "Elige tu idioma"
    override val languageSubtitle = "Puedes cambiarlo más tarde en los ajustes."

    override val libraryLocationTitle = "¿Dónde quieres guardar tu biblioteca?"
    override val libraryLocationSubtitle =
        "Elige una carpeta para tus partituras. Todo se guarda allí como archivos normales, así que puedes hacer copias de seguridad o moverlos cuando quieras."
    override val chooseFolder = "Elegir carpeta"
    override val changeFolder = "Cambiar carpeta"
    override val folderPickerFailed = "No se pudo abrir la carpeta. Inténtalo de nuevo."

    override val libraryTitle = "Biblioteca"
    override val libraryEmptyTitle = "Tu biblioteca está vacía"
    override val libraryEmptyText = "Las partituras que importes aparecerán aquí."

    override val cancel = "Cancelar"
    override val create = "Crear"

    override val newFolder = "Nueva carpeta"
    override val importScores = "Importar partituras"
    override val newFolderTitle = "Crear carpeta nueva"
    override val folderNameLabel = "Nombre de la carpeta"
    override val nameEmpty = "Introduce un nombre."
    override val nameInvalid = "El nombre no puede contener ninguno de estos caracteres: \\ / : * ? \" < > |"
    override val nameExists = "Este nombre ya existe en esta carpeta."

    override val importTypeTitle = "¿Qué quieres importar?"
    override val importTypeSubtitle = "Elige el formato de tus partituras."
    override val importTypeImages = "Imágenes"
    override val importTypeImagesText = "Una o varias imágenes, p. ej. páginas escaneadas (PNG, JPG, WebP, BMP)."
    override val importTypePdf = "PDF"
    override val importTypePdfText = "Un único documento PDF."
    override val importTypeMusicXml = "MusicXML"
    override val importTypeMusicXmlText = "Partituras digitales que también se pueden reproducir."
    override val comingSoon = "Próximamente"

    override val importFilesTitle = "Elige tus imágenes"
    override val importFilesSubtitle = "Ordena las páginas correctamente."
    override val addImages = "Añadir imágenes"
    override val moveUp = "Subir"
    override val moveDown = "Bajar"
    override val remove = "Quitar"

    override val importPreviewTitle = "Vista previa"
    override val importPreviewSubtitle = "Así se verá tu partitura."
    override val previewFailed = "No se pudieron cargar las imágenes."

    override val importMetadataTitle = "Detalles"
    override val importMetadataSubtitle = "Ponle un título a tu partitura para encontrarla después."
    override val titleLabel = "Título"
    override val composerLabel = "Compositor"
    override val descriptionLabel = "Descripción"
    override val importAction = "Importar"
    override val importFailed = "No se pudo guardar la partitura. Inténtalo de nuevo."
}
