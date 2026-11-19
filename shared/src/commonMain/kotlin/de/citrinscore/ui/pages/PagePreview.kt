package de.citrinscore.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.theme.Spacing

@Composable
fun PagePreview(
    pages: List<PreviewPage>,
    isLoading: Boolean,
    failed: Boolean,
    modifier: Modifier = Modifier,
    label: String = LocalStrings.current.previewLabel,
    failureMessage: String = LocalStrings.current.previewFailed,
    controls: @Composable () -> Unit = {},
) {
    PreviewFrame(isLoading, failed, modifier, label, failureMessage, controls) {
        val maxWidth = pages.maxOfOrNull { it.crop.size.width } ?: 1f
        pages.forEach { page ->
            Image(
                page.image,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(page.crop.size.width / maxWidth),
                contentScale = ContentScale.FillWidth,
            )
        }
    }
}

@Composable
fun PreviewFrame(
    isLoading: Boolean,
    failed: Boolean,
    modifier: Modifier = Modifier,
    label: String = LocalStrings.current.previewLabel,
    failureMessage: String = LocalStrings.current.previewFailed,
    controls: @Composable () -> Unit = {},
    pages: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
        Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.fillMaxWidth())
        controls()
        Column(
            modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Color.White).padding(Spacing.ExtraSmall),
            verticalArrangement = Arrangement.spacedBy(Spacing.Small),
            content = pages,
        )
        if (isLoading) CircularProgressIndicator()
        if (failed) Text(failureMessage, color = MaterialTheme.colorScheme.error)
    }
}
