package de.citrinscore.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import de.citrinscore.ui.theme.Spacing

@Composable
fun <T> TileGrid(
    items: List<T>,
    minTileWidth: Dp,
    modifier: Modifier = Modifier,
    spacing: Dp = Spacing.Large,
    tile: @Composable (item: T, modifier: Modifier) -> Unit,
) {
    BoxWithConstraints(modifier) {
        val count = items.size.coerceAtLeast(1)
        val fitting = ((maxWidth + spacing) / (minTileWidth + spacing)).toInt().coerceIn(1, count)
        val rows = (count + fitting - 1) / fitting
        val columns = (count + rows - 1) / rows
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            items.chunked(columns).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    row.forEach { item -> tile(item, Modifier.weight(1f).fillMaxHeight()) }
                }
            }
        }
    }
}
