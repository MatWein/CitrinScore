package de.citrinscore.ui.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.app_icon
import de.citrinscore.resources.ic_edit_note
import de.citrinscore.resources.ic_library_music
import de.citrinscore.resources.ic_play
import de.citrinscore.ui.components.IconBadge
import de.citrinscore.ui.components.StepHeader
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.components.TileGrid
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private data class Feature(val icon: DrawableResource, val title: String, val text: String)

@Composable
fun WelcomeStep() {
    val strings = LocalStrings.current
    val features = listOf(
        Feature(Res.drawable.ic_library_music, strings.featureLibraryTitle, strings.featureLibraryText),
        Feature(Res.drawable.ic_edit_note, strings.featureNotesTitle, strings.featureNotesText),
        Feature(Res.drawable.ic_play, strings.featurePlaybackTitle, strings.featurePlaybackText),
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        Image(painterResource(Res.drawable.app_icon), contentDescription = null, modifier = Modifier.size(160.dp))
        StepHeader(strings.welcomeTitle, strings.welcomeSubtitle)
        TileGrid(features, minTileWidth = 220.dp) { feature, modifier ->
            FeatureTile(feature, modifier)
        }
    }
}

@Composable
private fun FeatureTile(feature: Feature, modifier: Modifier) {
    Tile(modifier) {
        IconBadge(feature.icon)
        Text(feature.title, style = MaterialTheme.typography.titleLarge)
        Text(feature.text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
