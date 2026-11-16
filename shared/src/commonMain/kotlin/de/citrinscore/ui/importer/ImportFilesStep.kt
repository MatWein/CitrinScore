package de.citrinscore.ui.importer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
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
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val RowPadding = PaddingValues(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
private val CompactRowWidth = 360.dp
private val SideBySideWidth = 720.dp
private val SingleColumnWidth = 640.dp
private val SectionSpacing = 32.dp
private const val FileListWeight = 2f
private const val PreviewWeight = 3f

@Composable
fun ImportFilesStep(
    state: ImportState,
    onAdd: (List<PlatformFile>) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.importFilesTitle, strings.importFilesSubtitle)
        BoxWithConstraints(contentAlignment = Alignment.TopCenter) {
            val fileList: @Composable (Modifier) -> Unit = { modifier -> FileList(state, onAdd, onMove, onRemove, modifier) }
            val preview: @Composable (Modifier) -> Unit = { modifier -> if (state.files.isNotEmpty()) PagePreview(state, modifier) }
            if (maxWidth >= SideBySideWidth) {
                Row(horizontalArrangement = Arrangement.spacedBy(SectionSpacing)) {
                    fileList(Modifier.weight(FileListWeight))
                    preview(Modifier.weight(PreviewWeight))
                }
            } else {
                Column(Modifier.widthIn(max = SingleColumnWidth), verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
                    fileList(Modifier.fillMaxWidth())
                    preview(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun FileList(
    state: ImportState,
    onAdd: (List<PlatformFile>) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    modifier: Modifier,
) {
    val strings = LocalStrings.current
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(state.type?.extensions.orEmpty()),
        mode = FileKitMode.Multiple(),
    ) { picked ->
        picked?.let(onAdd)
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        state.files.forEachIndexed { index, file ->
            FileRow(
                position = index + 1,
                name = file.name,
                failed = file.path in state.failedPreviews,
                onMoveUp = { onMove(index, index - 1) }.takeIf { index > 0 },
                onMoveDown = { onMove(index, index + 1) }.takeIf { index < state.files.lastIndex },
                onRemove = { onRemove(index) },
            )
        }
        OutlinedButton(
            onClick = picker::launch,
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(strings.addImages)
        }
    }
}

@Composable
private fun PagePreview(state: ImportState, modifier: Modifier) {
    val strings = LocalStrings.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(strings.previewLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        Column(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)) {
            state.pages.forEach { page ->
                Image(page, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
            }
        }
        if (state.isLoadingPreview) CircularProgressIndicator()
        if (state.previewFailed) Text(strings.previewFailed, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun FileRow(position: Int, name: String, failed: Boolean, onMoveUp: (() -> Unit)?, onMoveDown: (() -> Unit)?, onRemove: () -> Unit) {
    val strings = LocalStrings.current
    val label: @Composable RowScope.() -> Unit = {
        Text(
            position.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            color = if (failed) MaterialTheme.colorScheme.error else Color.Unspecified,
            maxLines = 2,
            overflow = TextOverflow.MiddleEllipsis,
            modifier = Modifier.weight(1f),
        )
    }
    val actions: @Composable () -> Unit = {
        RowAction(Res.drawable.ic_arrow_upward, strings.moveUp, onMoveUp)
        RowAction(Res.drawable.ic_arrow_downward, strings.moveDown, onMoveDown)
        RowAction(Res.drawable.ic_close, strings.remove, onRemove)
    }

    Tile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, contentPadding = RowPadding) {
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
