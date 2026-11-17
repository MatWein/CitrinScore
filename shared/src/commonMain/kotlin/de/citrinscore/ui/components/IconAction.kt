package de.citrinscore.ui.components

import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

@Composable
fun IconAction(
    icon: DrawableResource,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: Boolean = false,
) {
    LabelTooltip(label) {
        IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
            BadgedBox(badge = { if (badge) Badge() }) {
                Icon(painterResource(icon), contentDescription = label)
            }
        }
    }
}
