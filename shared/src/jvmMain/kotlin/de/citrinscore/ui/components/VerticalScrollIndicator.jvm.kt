package de.citrinscore.ui.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun VerticalScrollIndicator(scrollState: ScrollState, modifier: Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    VerticalScrollbar(
        adapter = rememberScrollbarAdapter(scrollState),
        modifier = modifier,
        style = LocalScrollbarStyle.current.copy(
            thickness = 8.dp,
            shape = CircleShape,
            unhoverColor = onSurface.copy(alpha = 0.25f),
            hoverColor = onSurface.copy(alpha = 0.5f),
        ),
    )
}
