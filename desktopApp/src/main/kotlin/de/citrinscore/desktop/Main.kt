package de.citrinscore.desktop

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.slf4j.LoggerFactory
import java.awt.Dimension
import java.io.File

private const val SETTINGS_DIRECTORY = ".citrinscore"
private const val HOME_PROPERTY = "citrinscore.home"
private const val TITLE_SEPARATOR = " – "
private val MinimumWindowSize = Dimension(300, 600)
private val InitialWindowSize = DpSize(1280.dp, 800.dp)

fun main() {
    val filesDir = System.getProperty(HOME_PROPERTY)?.let(::File) ?: File(System.getProperty("user.home"), SETTINGS_DIRECTORY)
    System.setProperty(HOME_PROPERTY, filesDir.absolutePath)
    val logger = LoggerFactory.getLogger("de.citrinscore.desktop")
    Thread.setDefaultUncaughtExceptionHandler { thread, e -> logger.error("Uncaught exception in thread {}", thread.name, e) }
    logger.info("Starting {} {} on {} {} with Java {}", AppInfo.NAME, AppInfo.VERSION, System.getProperty("os.name"), System.getProperty("os.version"), System.getProperty("java.version"))
    FileKit.init(appId = AppInfo.ID, filesDir = filesDir)
    application(exitProcessOnExit = false) {
        var screenTitle by remember { mutableStateOf<String?>(null) }
        Window(
            onCloseRequest = {
                logger.info("Closing {}", AppInfo.NAME)
                exitApplication()
            },
            title = listOfNotNull(AppInfo.NAME, screenTitle).joinToString(TITLE_SEPARATOR),
            icon = painterResource(Res.drawable.app_icon),
            state = rememberWindowState(
                placement = WindowPlacement.Maximized,
                position = WindowPosition(Alignment.Center),
                size = InitialWindowSize,
            ),
        ) {
            LaunchedEffect(window) {
                window.minimumSize = MinimumWindowSize
            }
            App(onWindowTitleChange = { screenTitle = it })
        }
    }
}
