package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_image
import de.citrinscore.resources.ic_picture_as_pdf
import de.citrinscore.ui.components.IconBadge
import de.citrinscore.ui.components.StepHeader
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.components.TileGrid

@Composable
fun ImportTypeStep(selected: ImportType?, onSelect: (ImportType) -> Unit) {
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.importTypeTitle, strings.importTypeSubtitle)
        TileGrid(ImportType.entries, minTileWidth = 220.dp) { type, modifier ->
            ImportTypeTile(type, selected = type == selected, onClick = { onSelect(type) }, modifier)
        }
    }
}

@Composable
private fun ImportTypeTile(type: ImportType, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    val strings = LocalStrings.current
    Tile(modifier = modifier, selected = selected, onClick = onClick) {
        IconBadge(type.icon)
        Text(type.title(strings), style = MaterialTheme.typography.titleLarge)
        Text(type.description(strings), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val ImportType.icon
    get() = when (this) {
        ImportType.Images -> Res.drawable.ic_image
        ImportType.Pdf -> Res.drawable.ic_picture_as_pdf
    }

private fun ImportType.title(strings: Strings) = when (this) {
    ImportType.Images -> strings.importTypeImages
    ImportType.Pdf -> strings.importTypePdf
}

private fun ImportType.description(strings: Strings) = when (this) {
    ImportType.Images -> strings.importTypeImagesText
    ImportType.Pdf -> strings.importTypePdfText
}
