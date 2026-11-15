package de.citrinscore.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.AppLanguage
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_check
import de.citrinscore.ui.components.StepHeader
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.components.TileGrid
import org.jetbrains.compose.resources.painterResource

private val OptionPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)

@Composable
fun LanguageStep(selected: AppLanguage, onSelect: (AppLanguage) -> Unit) {
    val strings = LocalStrings.current

    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(32.dp)) {
        StepHeader(strings.languageTitle, strings.languageSubtitle)
        TileGrid(AppLanguage.entries, minTileWidth = 190.dp, spacing = 12.dp) { language, modifier ->
            LanguageOption(language, selected = language == selected, onClick = { onSelect(language) }, modifier)
        }
    }
}

@Composable
private fun LanguageOption(language: AppLanguage, selected: Boolean, onClick: () -> Unit, modifier: Modifier) {
    Tile(modifier, selected, onClick, MaterialTheme.shapes.medium, OptionPadding) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                language.tag.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp),
            )
            Text(
                language.nativeName,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (selected) {
                Icon(
                    painterResource(Res.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}
