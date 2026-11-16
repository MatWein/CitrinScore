package de.citrinscore.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import de.citrinscore.ui.theme.Spacing

private val BorderWidth = 1.dp
private val SelectedBorderWidth = 2.dp

@Composable
fun Tile(
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.large,
    contentPadding: PaddingValues = PaddingValues(Spacing.ExtraLarge),
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(if (selected) colors.primaryContainer else colors.surfaceContainerLow)
    val borderColor by animateColorAsState(if (selected) colors.primary else colors.outlineVariant)
    val border = BorderStroke(if (selected) SelectedBorderWidth else BorderWidth, borderColor)
    val body: @Composable () -> Unit = {
        Column(
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
            content = content,
        )
    }

    if (onClick == null) {
        Surface(modifier, shape, container, border = border, content = body)
    } else {
        Surface(onClick, modifier, shape = shape, color = container, border = border, content = body)
    }
}
