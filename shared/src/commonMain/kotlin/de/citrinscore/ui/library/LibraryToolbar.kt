package de.citrinscore.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.resources.ic_create_new_folder
import de.citrinscore.resources.ic_upload_file
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val CompactWidth = 600.dp

@Composable
fun LibraryToolbar(
    folderName: String?,
    onUp: () -> Unit,
    onNewFolder: () -> Unit,
    onImport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val strings = LocalStrings.current
    Surface(modifier, shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(8.dp)) {
            val compact = maxWidth < CompactWidth
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (folderName != null) {
                    IconButton(onClick = onUp) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = strings.back)
                    }
                    Text(
                        folderName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                ToolbarAction(Res.drawable.ic_create_new_folder, strings.newFolder, compact, primary = false, onNewFolder)
                ToolbarAction(Res.drawable.ic_upload_file, strings.importScores, compact, primary = true, onImport)
            }
        }
    }
}

@Composable
private fun ToolbarAction(icon: DrawableResource, label: String, compact: Boolean, primary: Boolean, onClick: () -> Unit) {
    val painter = painterResource(icon)
    when {
        compact && primary -> FilledIconButton(onClick) { Icon(painter, contentDescription = label) }
        compact -> FilledTonalIconButton(onClick) { Icon(painter, contentDescription = label) }
        else -> {
            val content: @Composable () -> Unit = {
                Icon(painter, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(label)
            }
            if (primary) Button(onClick) { content() } else FilledTonalButton(onClick) { content() }
        }
    }
}
