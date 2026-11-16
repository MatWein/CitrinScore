package de.citrinscore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_check
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.theme.Spacing
import org.jetbrains.compose.resources.painterResource

private val CheckIconSize = 20.dp
private val OptionPadding = PaddingValues(horizontal = Spacing.Large, vertical = 14.dp)

@Composable
fun OptionTile(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable RowScope.() -> Unit,
) {
    Tile(modifier, selected, onClick, MaterialTheme.shapes.medium, OptionPadding) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
            leading()
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(CheckIconSize),
                )
            }
        }
    }
}
