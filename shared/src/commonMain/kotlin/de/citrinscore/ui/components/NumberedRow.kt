package de.citrinscore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_downward
import de.citrinscore.resources.ic_arrow_upward
import de.citrinscore.resources.ic_close
import de.citrinscore.resources.ic_edit
import de.citrinscore.ui.theme.Spacing

private val RowPadding = PaddingValues(start = Spacing.Page, end = Spacing.Small, top = Spacing.Small, bottom = Spacing.Small)
private val CompactRowWidth = 360.dp
private const val NAME_MAX_LINES = 2

@Composable
fun NumberedRow(
    position: Int,
    name: String,
    reorderable: Boolean,
    onMoveUp: (() -> Unit)?,
    onMoveDown: (() -> Unit)?,
    onRemove: (() -> Unit)?,
    failed: Boolean = false,
    subtitle: String? = null,
    onEdit: (() -> Unit)? = null,
) {
    val strings = LocalStrings.current
    val label: @Composable RowScope.() -> Unit = {
        Text(
            position.toString(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Column(Modifier.weight(1f)) {
            Text(
                name,
                style = MaterialTheme.typography.bodyLarge,
                color = if (failed) MaterialTheme.colorScheme.error else Color.Unspecified,
                maxLines = NAME_MAX_LINES,
                overflow = TextOverflow.MiddleEllipsis,
            )
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
    val actions: @Composable () -> Unit = {
        onEdit?.let { IconAction(Res.drawable.ic_edit, strings.edit, onClick = it) }
        if (reorderable) {
            IconAction(Res.drawable.ic_arrow_upward, strings.moveUp, onClick = { onMoveUp?.invoke() }, enabled = onMoveUp != null)
            IconAction(Res.drawable.ic_arrow_downward, strings.moveDown, onClick = { onMoveDown?.invoke() }, enabled = onMoveDown != null)
        }
        IconAction(Res.drawable.ic_close, strings.remove, onClick = { onRemove?.invoke() }, enabled = onRemove != null)
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
