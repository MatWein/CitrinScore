package de.citrinscore.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import de.citrinscore.App
import de.citrinscore.AppInfo
import de.citrinscore.resources.Res
import de.citrinscore.resources.app_icon
import io.github.vinceglb.filekit.FileKit
import org.jetbrains.compose.resources.painterResource
import java.awt.Dimension
import java.io.File

private val MinimumWindowSize = Dimension(300, 600)

fun main() {
    FileKit.init(appId = AppInfo.ID, filesDir = File(System.getProperty("user.home"), ".citrinscore"))
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = AppInfo.NAME,
            icon = painterResource(Res.drawable.app_icon),
            state = rememberWindowState(
                placement = WindowPlacement.Maximized,
                position = WindowPosition(Alignment.Center),
                size = DpSize(1280.dp, 800.dp),
            ),
        ) {
            LaunchedEffect(window) {
                window.minimumSize = MinimumWindowSize
            }
            App()
        }
    }
}
