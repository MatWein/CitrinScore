package de.citrinscore.ui.viewer.files

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import de.citrinscore.score.Attachment
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitDialogSettings
import io.github.vinceglb.filekit.dialogs.compose.rememberFileSaverLauncher

@Composable
fun rememberAttachmentSaver(onSave: (Attachment, PlatformFile) -> Unit): (Attachment) -> Unit {
    var saving by remember { mutableStateOf<Attachment?>(null) }
    val saver = rememberFileSaverLauncher(FileKitDialogSettings.createDefault()) { target ->
        val attachment = saving
        saving = null
        if (target != null && attachment != null) onSave(attachment, target)
    }
    return { attachment ->
        saving = attachment
        saver.launch(suggestedName = attachment.baseName, defaultExtension = attachment.extension.ifEmpty { null })
    }
}
