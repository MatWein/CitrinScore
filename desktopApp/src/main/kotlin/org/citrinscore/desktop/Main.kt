package org.citrinscore.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.citrinscore.App

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "citrinScore") {
        App()
    }
}
