package de.citrinscore.ui.settings

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_contrast
import de.citrinscore.resources.ic_dark_mode
import de.citrinscore.resources.ic_light_mode
import de.citrinscore.settings.ThemeMode
import de.citrinscore.ui.components.TileGrid
import de.citrinscore.ui.theme.Spacing
import de.citrinscore.ui.theme.TileWidth
import org.jetbrains.compose.resources.painterResource

@Composable
fun ThemeOptions(selected: ThemeMode, onSelect: (ThemeMode) -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    TileGrid(ThemeMode.entries, minTileWidth = TileWidth.Option, modifier = modifier, spacing = Spacing.Medium) { mode, tileModifier ->
        OptionTile(mode.label(strings), selected = mode == selected, onClick = { onSelect(mode) }, tileModifier) {
            Icon(painterResource(mode.icon), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

private val ThemeMode.icon
    get() = when (this) {
        ThemeMode.System -> Res.drawable.ic_contrast
        ThemeMode.Light -> Res.drawable.ic_light_mode
        ThemeMode.Dark -> Res.drawable.ic_dark_mode
    }

private fun ThemeMode.label(strings: Strings) = when (this) {
    ThemeMode.System -> strings.themeSystem
    ThemeMode.Light -> strings.themeLight
    ThemeMode.Dark -> strings.themeDark
}
