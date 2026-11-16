package de.citrinscore.ui.settings

import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.AppLanguage
import de.citrinscore.ui.components.TileGrid

@Composable
fun LanguageOptions(selected: AppLanguage, onSelect: (AppLanguage) -> Unit, modifier: Modifier = Modifier) {
    TileGrid(AppLanguage.entries, minTileWidth = 190.dp, modifier = modifier, spacing = 12.dp) { language, tileModifier ->
        OptionTile(language.nativeName, selected = language == selected, onClick = { onSelect(language) }, tileModifier) {
            Text(
                language.tag.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.width(28.dp),
            )
        }
    }
}
