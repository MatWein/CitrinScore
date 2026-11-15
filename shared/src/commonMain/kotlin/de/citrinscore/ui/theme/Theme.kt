package de.citrinscore.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = Color(0xFF8A5100),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF2C1600),
    secondary = Color(0xFF4F4A7A),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE4DFFF),
    onSecondaryContainer = Color(0xFF1E1A33),
    background = Color(0xFFFFF8F3),
    onBackground = Color(0xFF201A15),
    surface = Color(0xFFFFF8F3),
    onSurface = Color(0xFF201A15),
    surfaceVariant = Color(0xFFF2DFD1),
    onSurfaceVariant = Color(0xFF51443A),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFF1E6),
    surfaceContainer = Color(0xFFFBEBDD),
    surfaceContainerHigh = Color(0xFFF5E5D7),
    surfaceContainerHighest = Color(0xFFEFE0D1),
    outline = Color(0xFF837468),
    outlineVariant = Color(0xFFD6C3B5),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFB95E),
    onPrimary = Color(0xFF4A2800),
    primaryContainer = Color(0xFF6A3C00),
    onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFFC6C0F0),
    onSecondary = Color(0xFF2F2A55),
    secondaryContainer = Color(0xFF3A3560),
    onSecondaryContainer = Color(0xFFE4DFFF),
    background = Color(0xFF15121F),
    onBackground = Color(0xFFE8E0EE),
    surface = Color(0xFF15121F),
    onSurface = Color(0xFFE8E0EE),
    surfaceVariant = Color(0xFF2C2839),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF100D19),
    surfaceContainerLow = Color(0xFF1D1A29),
    surfaceContainer = Color(0xFF221E2E),
    surfaceContainerHigh = Color(0xFF2C2839),
    surfaceContainerHighest = Color(0xFF373344),
    outline = Color(0xFF948F99),
    outlineVariant = Color(0xFF49454F),
)

private val CitrinShapes = Shapes(extraLarge = RoundedCornerShape(20.dp))

@Composable
fun CitrinTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, shapes = CitrinShapes, content = content)
}
