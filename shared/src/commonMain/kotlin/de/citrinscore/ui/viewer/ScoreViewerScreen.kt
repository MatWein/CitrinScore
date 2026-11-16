package de.citrinscore.ui.viewer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.resources.ic_arrow_downward
import de.citrinscore.resources.ic_arrow_upward
import de.citrinscore.resources.ic_zoom_in
import de.citrinscore.resources.ic_zoom_out
import de.citrinscore.score.ScoreMetadata
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private const val SAVE_DELAY_MILLIS = 1000L

@Composable
fun ScoreViewerScreen(viewModel: ScoreViewModel, onClose: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val ready = state as? ScoreViewerState.Ready
    val viewport = ready?.let { remember(it) { ScoreViewport(it.view) } }
    val toolbar = ToolbarState(ready?.metadata, viewport, onClose)

    BoxWithConstraints(Modifier.fillMaxSize().background(Color.White)) {
        if (maxWidth > maxHeight) {
            Row(Modifier.fillMaxSize()) {
                SideToolbar(toolbar)
                val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.End + WindowInsetsSides.Vertical)
                ViewerContent(state, viewport, viewModel, Modifier.weight(1f).fillMaxHeight().windowInsetsPadding(insets))
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                TopToolbar(toolbar)
                val insets = WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                ViewerContent(state, viewport, viewModel, Modifier.weight(1f).windowInsetsPadding(insets))
            }
        }
    }
}

private class ToolbarState(val metadata: ScoreMetadata?, val viewport: ScoreViewport?, val onClose: () -> Unit)

@Composable
private fun ViewerContent(state: ScoreViewerState, viewport: ScoreViewport?, viewModel: ScoreViewModel, modifier: Modifier) {
    val strings = LocalStrings.current
    Box(modifier, contentAlignment = Alignment.Center) {
        when (state) {
            ScoreViewerState.Loading -> CircularProgressIndicator()
            ScoreViewerState.Failed -> Text(strings.scoreLoadFailed, color = MaterialTheme.colorScheme.error)
            is ScoreViewerState.Ready -> if (viewport != null) {
                ScoreDocument(state.pageSizes, viewport, viewModel::render)
                PersistView(viewport, viewModel)
            }
        }
    }
}

@Composable
private fun PersistView(viewport: ScoreViewport, viewModel: ScoreViewModel) {
    LaunchedEffect(viewport) {
        snapshotFlow { viewport.zoom to viewport.offset }.collectLatest {
            delay(SAVE_DELAY_MILLIS)
            viewModel.saveView(viewport.view)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { viewModel.saveView(viewport.view) }
    DisposableEffect(viewport) { onDispose { viewModel.saveView(viewport.view) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopToolbar(toolbar: ToolbarState) {
    TopAppBar(
        title = {
            Column {
                Text(toolbar.metadata?.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                toolbar.metadata?.composer?.takeIf { it.isNotEmpty() }?.let { composer ->
                    Text(
                        composer,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = { BackAction(toolbar) },
        actions = {
            PageActions(toolbar)
            ZoomActions(toolbar)
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    )
}

@Composable
private fun SideToolbar(toolbar: ToolbarState) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, modifier = Modifier.fillMaxHeight()) {
        Column(
            modifier = Modifier
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Start + WindowInsetsSides.Vertical))
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            BackAction(toolbar)
            PageActions(toolbar)
            ZoomActions(toolbar)
        }
    }
}

@Composable
private fun BackAction(toolbar: ToolbarState) {
    ToolbarAction(Res.drawable.ic_arrow_back, LocalStrings.current.back, enabled = true, onClick = toolbar.onClose)
}

@Composable
private fun PageActions(toolbar: ToolbarState) {
    val strings = LocalStrings.current
    val viewport = toolbar.viewport
    ToolbarAction(Res.drawable.ic_arrow_upward, strings.previousPage, enabled = viewport?.canGoToPreviousPage == true) {
        viewport?.previousPage()
    }
    ToolbarAction(Res.drawable.ic_arrow_downward, strings.nextPage, enabled = viewport?.canGoToNextPage == true) {
        viewport?.nextPage()
    }
}

@Composable
private fun ZoomActions(toolbar: ToolbarState) {
    val strings = LocalStrings.current
    val viewport = toolbar.viewport
    ToolbarAction(Res.drawable.ic_zoom_out, strings.zoomOut, enabled = viewport?.canZoomOut == true) {
        viewport?.zoomBy(1 / ScoreViewport.ZOOM_STEP)
    }
    ToolbarAction(Res.drawable.ic_zoom_in, strings.zoomIn, enabled = viewport?.canZoomIn == true) {
        viewport?.zoomBy(ScoreViewport.ZOOM_STEP)
    }
}

@Composable
private fun ToolbarAction(icon: DrawableResource, label: String, enabled: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(painterResource(icon), contentDescription = label)
    }
}
