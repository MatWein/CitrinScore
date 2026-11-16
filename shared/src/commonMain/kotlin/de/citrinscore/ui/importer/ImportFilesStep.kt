package de.citrinscore.ui.importer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
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
import de.citrinscore.score.scan.ScanThresholdRange
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path
import org.jetbrains.compose.resources.painterResource

private val RowPadding = PaddingValues(start = Spacing.Page, end = Spacing.Small, top = Spacing.Small, bottom = Spacing.Small)
private val AddButtonPadding = PaddingValues(horizontal = Spacing.ExtraLarge, vertical = 14.dp)
private val CompactRowWidth = 360.dp
private val SideBySideWidth = 720.dp
private val SectionSpacing = Spacing.Section
private val PreviewMargin = Spacing.ExtraSmall
private val PreviewGap = Spacing.Small
private const val FileListWeight = 2f
private const val PreviewWeight = 3f

@Composable
fun ImportFilesStep(
    state: ImportState,
    onAdd: (List<PlatformFile>) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
    onScanThresholdChange: (Float) -> Unit,
) {
    val strings = LocalStrings.current
    val (title, subtitle) = when (state.type) {
        ImportType.Pdf -> strings.importPdfTitle to strings.importPdfSubtitle
        ImportType.Scan -> strings.importScanTitle to strings.importScanSubtitle
        ImportType.Images, null -> strings.importFilesTitle to strings.importFilesSubtitle
    }
    WizardStep(title, subtitle) {
        BoxWithConstraints(contentAlignment = Alignment.TopCenter) {
            val fileList: @Composable (Modifier) -> Unit = { modifier -> FileList(state, onAdd, onMove, onRemove, modifier) }
            val preview: @Composable (Modifier) -> Unit = { modifier ->
                if (state.files.isNotEmpty()) PagePreview(state, onScanThresholdChange, modifier)
            }
            if (maxWidth >= SideBySideWidth) {
                Row(horizontalArrangement = Arrangement.spacedBy(SectionSpacing)) {
                    fileList(Modifier.weight(FileListWeight))
                    preview(Modifier.weight(PreviewWeight))
                }
            } else {
                Column(Modifier.widthIn(max = ContentWidth.Form), verticalArrangement = Arrangement.spacedBy(SectionSpacing)) {
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
    val isPdf = state.type == ImportType.Pdf
    val picker = rememberFilePickerLauncher(
        type = FileKitType.File(state.type?.extensions.orEmpty()),
        mode = FileKitMode.Multiple(maxItems = if (isPdf) 1 else null),
    ) { picked ->
        picked?.let(onAdd)
    }
    val scanLauncher = rememberScanLauncher(onAdd)
    val (addLabel, onAddClick) = when (state.type) {
        ImportType.Pdf -> strings.choosePdf to picker::launch
        ImportType.Scan -> (if (scanUsesCamera) strings.takePhoto else strings.addScans) to scanLauncher
        ImportType.Images, null -> strings.addImages to picker::launch
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        state.files.forEachIndexed { index, file ->
            FileRow(
                position = index + 1,
                name = file.name,
                failed = file.path in state.failedPreviews,
                reorderable = !isPdf,
                onMoveUp = { onMove(index, index - 1) }.takeIf { index > 0 },
                onMoveDown = { onMove(index, index + 1) }.takeIf { index < state.files.lastIndex },
                onRemove = { onRemove(index) },
            )
        }
        OutlinedButton(
            onClick = onAddClick,
            contentPadding = AddButtonPadding,
            modifier = Modifier.padding(top = Spacing.Large),
        ) {
            Icon(painterResource(Res.drawable.ic_add), contentDescription = null)
            Spacer(Modifier.width(Spacing.Small))
            Text(addLabel)
        }
    }
}

@Composable
private fun PagePreview(state: ImportState, onScanThresholdChange: (Float) -> Unit, modifier: Modifier) {
    val strings = LocalStrings.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        Text(strings.previewLabel, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        if (state.type == ImportType.Scan) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Large)) {
                Text(strings.scanThreshold, style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = state.scanThreshold,
                    onValueChange = onScanThresholdChange,
                    valueRange = ScanThresholdRange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        val pages = state.pages
        val maxWidth = pages.maxOfOrNull { it.crop.size.width } ?: 1f
        Column(
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Color.White).padding(PreviewMargin),
            verticalArrangement = Arrangement.spacedBy(PreviewGap),
        ) {
            pages.forEach { page ->
                Image(
                    page.image,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth(page.crop.size.width / maxWidth),
                    contentScale = ContentScale.FillWidth,
                )
            }
        }
        if (state.isLoadingPreview) CircularProgressIndicator()
        if (state.previewFailed) Text(strings.previewFailed, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun FileRow(
    position: Int,
    name: String,
    failed: Boolean,
    reorderable: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: () -> Unit,
) {
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
        if (reorderable) {
            IconAction(Res.drawable.ic_arrow_upward, strings.moveUp, onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null)
            IconAction(Res.drawable.ic_arrow_downward, strings.moveDown, onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null)
        }
        IconAction(Res.drawable.ic_close, strings.remove, onClick = onRemove)
    }

    Tile(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium, contentPadding = RowPadding) {
        BoxWithConstraints {
            if (maxWidth < CompactRowWidth) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium), content = label)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { actions() }
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
                    label()
                    actions()
                }
            }
        }
    }
}
