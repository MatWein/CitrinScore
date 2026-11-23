package de.citrinscore.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

/**
 * Focuses the element and shows the software keyboard as soon as it appears, for example the text field of a dialog.
 * The request waits one frame so that a dialog window is attached and can take the focus.
 */
@Composable
fun Modifier.initialFocus(): Modifier {
    val requester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(requester) {
        withFrameNanos {}
        requester.requestFocus()
        keyboard?.show()
    }
    return focusRequester(requester)
}
