package de.citrinscore.ui.importer

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_document_scanner
import de.citrinscore.resources.ic_image
import de.citrinscore.resources.ic_music_note
import de.citrinscore.resources.ic_picture_as_pdf
import de.citrinscore.ui.components.IconBadge
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.components.TileGrid
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.theme.TileWidth

@Composable
fun ImportTypeStep(selected: ImportType?, onSelect: (ImportType) -> Unit) {
    val strings = LocalStrings.current
    WizardStep(strings.importTypeTitle, strings.importTypeSubtitle) {
        val scanAvailable = isScanAvailable()
        TileGrid(ImportType.entries, minTileWidth = TileWidth.Feature) { type, modifier ->
            val available = type != ImportType.Scan || scanAvailable
            ImportTypeTile(type, selected = type == selected, available = available, onClick = { onSelect(type) }, modifier)
        }
    }
}

@Composable
private fun ImportTypeTile(type: ImportType, selected: Boolean, available: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val strings = LocalStrings.current
    Tile(
        modifier = modifier.alpha(if (available) 1f else UNAVAILABLE_ALPHA),
        selected = selected,
        onClick = onClick.takeIf { available },
    ) {
        IconBadge(type.icon)
        Text(type.title(strings), style = MaterialTheme.typography.titleLarge)
        Text(type.description(strings), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (!available) {
            Text(strings.noCameraAvailable, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private const val UNAVAILABLE_ALPHA = 0.5f

private val ImportType.icon
    get() = when (this) {
        ImportType.Images -> Res.drawable.ic_image
        ImportType.Scan -> Res.drawable.ic_document_scanner
        ImportType.Pdf -> Res.drawable.ic_picture_as_pdf
        ImportType.MusicXml -> Res.drawable.ic_music_note
    }

internal fun ImportType.title(strings: Strings) = when (this) {
    ImportType.Images -> strings.importTypeImages
    ImportType.Scan -> strings.importTypeScan
    ImportType.Pdf -> strings.importTypePdf
    ImportType.MusicXml -> strings.importTypeMusicXml
}

private fun ImportType.description(strings: Strings) = when (this) {
    ImportType.Images -> strings.importTypeImagesText
    ImportType.Scan -> if (scanUsesCamera) strings.importTypeScanCameraText else strings.importTypeScanFilesText
    ImportType.Pdf -> strings.importTypePdfText
    ImportType.MusicXml -> strings.importTypeMusicXmlText
}
