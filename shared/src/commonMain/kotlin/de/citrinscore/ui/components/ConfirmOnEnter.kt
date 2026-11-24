package de.citrinscore.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

/**
 * Confirms the dialog of a text field when Enter is pressed on a hardware keyboard, like on the desktop. Shift+Enter
 * still starts a new line in a text field with several lines. Nothing happens while the dialog cannot be confirmed.
 */
fun Modifier.confirmOnEnter(enabled: Boolean, onConfirm: () -> Unit): Modifier = onPreviewKeyEvent { event ->
    val isEnter = event.key == Key.Enter || event.key == Key.NumPadEnter
    if (!isEnter || event.isShiftPressed) return@onPreviewKeyEvent false
    if (event.type == KeyEventType.KeyDown && enabled) onConfirm()
    true
}
