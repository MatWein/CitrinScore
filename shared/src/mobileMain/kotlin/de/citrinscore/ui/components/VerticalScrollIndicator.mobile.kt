package de.citrinscore.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
actual fun VerticalScrollIndicator(scrollState: ScrollState, modifier: Modifier) = Unit

@Composable
actual fun VerticalScrollIndicator(gridState: LazyGridState, modifier: Modifier) = Unit

@Composable
actual fun VerticalScrollIndicator(listState: LazyListState, modifier: Modifier) = Unit
