package de.citrinscore.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import de.citrinscore.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "citrinScore") {
        App()
    }
}
