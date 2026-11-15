package de.citrinscore.ui.library

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.app_icon
import de.citrinscore.resources.ic_library_music
import de.citrinscore.settings.AppSettings
import de.citrinscore.ui.components.IconBadge
import org.jetbrains.compose.resources.painterResource

@Composable
fun LibraryScreen(settings: AppSettings) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp)) {
        LibraryHeader(settings.library.displayPath)
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            EmptyLibrary()
        }
    }
}

@Composable
private fun LibraryHeader(location: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Image(painterResource(Res.drawable.app_icon), contentDescription = null, modifier = Modifier.size(48.dp))
        Column {
            Text(LocalStrings.current.libraryTitle, style = MaterialTheme.typography.headlineMedium)
            Text(
                location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.MiddleEllipsis,
            )
        }
    }
}

@Composable
private fun EmptyLibrary() {
    val strings = LocalStrings.current
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        IconBadge(Res.drawable.ic_library_music, size = 112.dp)
        Text(strings.libraryEmptyTitle, style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Text(
            strings.libraryEmptyText,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
