package de.citrinscore.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.AddButton
import de.citrinscore.ui.components.NumberedRow
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.pages.PagePreview
import de.citrinscore.ui.pages.PagesLayout
import de.citrinscore.ui.parts.PartNameField
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.path

@Composable
fun ImportFilesStep(draft: PartDraft, nameTaken: Boolean, editor: PartDraftEditor) {
    val strings = LocalStrings.current
    val (title, subtitle) = when (draft.type) {
        ImportType.Pdf -> strings.importPdfTitle to strings.importPdfSubtitle
        ImportType.MusicXml -> strings.importMusicXmlTitle to strings.importMusicXmlSubtitle
        ImportType.Scan -> strings.importScanTitle to strings.importScanSubtitle
        ImportType.Images, null -> strings.importFilesTitle to strings.importFilesSubtitle
    }
    WizardStep(title, subtitle) {
        PartNameField(draft.name, nameTaken, editor::updateName)
        PagesLayout(
            list = { modifier -> FileList(draft, editor, modifier) },
            preview = { modifier ->
                when {
                    draft.files.isEmpty() -> Unit
                    draft.type == ImportType.Scan -> ScanPreviewPanel(draft, editor, modifier)
                    else -> PagePreview(draft.pages, draft.isLoadingPreview, draft.previewFailed, modifier)
                }
            },
        )
    }
}

@Composable
private fun FileList(draft: PartDraft, editor: PartDraftEditor, modifier: Modifier) {
    val strings = LocalStrings.current
    val singleFile = draft.type?.singleFile == true
    val picker = rememberFilePickerLauncher(
        type = draft.type?.pickerExtensions?.let { FileKitType.File(it) } ?: FileKitType.File(),
        mode = FileKitMode.Multiple(maxItems = if (singleFile) 1 else null),
    ) { picked ->
        picked?.let(editor::addFiles)
    }
    val scanLauncher = rememberScanLauncher(editor::addFiles)
    val (addLabel, onAddClick) = when (draft.type) {
        ImportType.Pdf -> strings.choosePdf to picker::launch
        ImportType.MusicXml -> strings.chooseMusicXml to picker::launch
        ImportType.Scan -> (if (scanUsesCamera) strings.takePhoto else strings.addScans) to scanLauncher
        ImportType.Images, null -> strings.addImages to picker::launch
    }

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
        draft.files.forEachIndexed { index, file ->
            NumberedRow(
                position = index + 1,
                name = file.name,
                reorderable = !singleFile,
                onMoveUp = { editor.moveFile(index, index - 1) }.takeIf { index > 0 },
                onMoveDown = { editor.moveFile(index, index + 1) }.takeIf { index < draft.files.lastIndex },
                onRemove = { editor.removeFile(index) },
                failed = file.path in draft.failedPreviews,
            )
        }
        AddButton(addLabel, onAddClick)
    }
}

