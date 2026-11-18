package de.citrinscore.ui.components

import androidx.compose.foundation.LocalScrollbarStyle
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.VerticalScrollbar
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.v2.ScrollbarAdapter
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
actual fun VerticalScrollIndicator(scrollState: ScrollState, modifier: Modifier) =
    ThemedScrollbar(rememberScrollbarAdapter(scrollState), modifier)

@Composable
actual fun VerticalScrollIndicator(gridState: LazyGridState, modifier: Modifier) =
    ThemedScrollbar(rememberScrollbarAdapter(gridState), modifier)

@Composable
actual fun VerticalScrollIndicator(listState: LazyListState, modifier: Modifier) =
    ThemedScrollbar(rememberScrollbarAdapter(listState), modifier)

@Composable
private fun ThemedScrollbar(adapter: ScrollbarAdapter, modifier: Modifier) {
    val onSurface = MaterialTheme.colorScheme.onSurface
    VerticalScrollbar(
        adapter = adapter,
        modifier = modifier,
        style = LocalScrollbarStyle.current.copy(
            thickness = 8.dp,
            shape = CircleShape,
            unhoverColor = onSurface.copy(alpha = 0.25f),
            hoverColor = onSurface.copy(alpha = 0.5f),
        ),
    )
}
