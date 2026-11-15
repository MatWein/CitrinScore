package de.citrinscore.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.citrinscore.App
import de.citrinscore.desktop.resources.Res
import de.citrinscore.desktop.resources.app_icon
import org.jetbrains.compose.resources.painterResource

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "citrinScore",
        icon = painterResource(Res.drawable.app_icon),
    ) {
        App()
    }
}
