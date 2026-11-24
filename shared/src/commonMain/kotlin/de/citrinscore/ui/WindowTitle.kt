package de.citrinscore.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf

/** Receives what the current screen wants to show in the window title, or null for the app name alone. */
val LocalWindowTitle = staticCompositionLocalOf<(String?) -> Unit> { {} }

@Composable
fun WindowTitle(title: String?) {
    val onTitleChange = LocalWindowTitle.current
    DisposableEffect(title, onTitleChange) {
        onTitleChange(title)
        onDispose { onTitleChange(null) }
    }
}
