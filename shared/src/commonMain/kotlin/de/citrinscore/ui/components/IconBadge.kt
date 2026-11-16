package de.citrinscore.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val BACKGROUND_ALPHA = 0.14f
private const val ICON_SCALE = 0.5f

object IconBadgeSize {
    val Regular = 56.dp
    val Large = 72.dp
    val Hero = 112.dp
}

@Composable
fun IconBadge(icon: DrawableResource, modifier: Modifier = Modifier, size: Dp = IconBadgeSize.Regular) {
    Box(
        modifier = modifier
            .size(size)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = BACKGROUND_ALPHA), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(size * ICON_SCALE),
        )
    }
}
