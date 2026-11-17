package de.citrinscore.ui.parts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.AddButton
import de.citrinscore.ui.components.NumberedRow
import de.citrinscore.ui.components.WizardStep
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing

class PartRowModel(val name: String, val summary: String)

@Composable
fun PartsStep(
    subtitle: String,
    parts: List<PartRowModel>,
    failure: String?,
    onAdd: () -> Unit,
    onEdit: (Int) -> Unit,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (Int) -> Unit,
) {
    val strings = LocalStrings.current
    WizardStep(strings.parts, subtitle) {
        Column(
            modifier = Modifier.widthIn(max = ContentWidth.Form).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            parts.forEachIndexed { index, part ->
                NumberedRow(
                    position = index + 1,
                    name = part.name,
                    subtitle = part.summary,
                    reorderable = true,
                    onEdit = { onEdit(index) },
                    onMoveUp = { onMove(index, index - 1) }.takeIf { index > 0 },
                    onMoveDown = { onMove(index, index + 1) }.takeIf { index < parts.lastIndex },
                    onRemove = { onRemove(index) }.takeIf { parts.size > 1 },
                )
            }
            AddButton(strings.addPart, onAdd)
            failure?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }
}
