package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_add
import de.citrinscore.resources.ic_arrow_downward
import de.citrinscore.resources.ic_arrow_upward
import de.citrinscore.resources.ic_close
import de.citrinscore.ui.components.StepHeader
import de.citrinscore.ui.components.Tile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val RowShape = RoundedCornerShape(20.dp)
private val RowPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
private val CompactRowWidth = 440.dp

@Composable
fun ImportFilesStep(
    files: List<PlatformFile>,
    extensions: List<String>,
    onAdd: (List<PlatformFile>) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    val picker = rememberFilePickerLauncher(type = FileKitType.File(extensions), mode = FileKitMode.Multiple()) { picked ->
        picked?.let(onAdd)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.importFilesTitle, strings.importFilesSubtitle)
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            files.forEachIndexed { index, file ->
                FileRow(
                    position = index + 1,
                    name = file.name,
                    onMoveUp = { onMove(index, index - 1) }.takeIf { index > 0 },
                    onMoveDown = { onMove(index, index + 1) }.takeIf { index < files.lastIndex },
                    onRemove = { onRemove(index) },
                )
            }
        }
        OutlinedButton(onClick = picker::launch, contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)) {
            Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.addImages)
        }
    }
}

@Composable
private fun FileRow(position: Int, name: String, onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?, onRemove: () -> Unit) {
    val strings = LocalStrings.current
    val label: @Composable RowScope.() -> Unit = {
        Text(
            position.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.MiddleEllipsis, modifier = Modifier.weight(1f))
    }
    val actions: @Composable () -> Unit = {
        RowAction(Res.drawable.ic_arrow_upward, strings.moveUp, onMoveUp)
        RowAction(Res.drawable.ic_arrow_downward, strings.moveDown, onMoveDown)
        RowAction(Res.drawable.ic_close, strings.remove, onRemove)
    }

    Tile(Modifier.fillMaxWidth(), shape = RowShape, contentPadding = RowPadding) {
        BoxWithConstraints {
            if (maxWidth < CompactRowWidth) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), content = label)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    label()
                    actions()
                }
            }
        }
    }
}

@Composable
private fun RowAction(icon: DrawableResource, label: String, onClick: (() -> Unit)?) {
    IconButton(onClick = { onClick?.invoke() }, enabled = onClick != null) {
        Icon(painterResource(icon), contentDescription = label)
    }
}
