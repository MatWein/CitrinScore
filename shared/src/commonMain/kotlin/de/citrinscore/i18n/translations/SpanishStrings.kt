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
}
