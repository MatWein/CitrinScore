package de.citrinscore.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
expect fun VerticalScrollIndicator(scrollState: ScrollState, modifier: Modifier = Modifier)

@Composable
expect fun VerticalScrollIndicator(gridState: LazyGridState, modifier: Modifier = Modifier)
