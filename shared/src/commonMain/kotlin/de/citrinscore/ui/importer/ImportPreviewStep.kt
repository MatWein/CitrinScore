package de.citrinscore.ui.importer

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.ui.components.StepHeader

@Composable
fun ImportPreviewStep(pages: List<ImageBitmap>, isLoading: Boolean, failed: Boolean) {
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.importPreviewTitle, strings.importPreviewSubtitle)
        when {
            isLoading -> CircularProgressIndicator()
            failed -> Text(strings.previewFailed, color = MaterialTheme.colorScheme.error)
            else -> Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))) {
                pages.forEach { page ->
                    Image(page, contentDescription = null, modifier = Modifier.fillMaxWidth(), contentScale = ContentScale.FillWidth)
                }
            }
        }
    }
}
