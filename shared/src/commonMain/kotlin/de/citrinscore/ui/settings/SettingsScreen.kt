package de.citrinscore.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.citrinscore.AppInfo
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.i18n.Strings
import de.citrinscore.library.LibraryMoveResult
import de.citrinscore.resources.Res
import de.citrinscore.resources.app_icon
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.settings.AppSettings
import de.citrinscore.settings.LibraryLocation
import de.citrinscore.settings.LibraryTarget
import de.citrinscore.sync.librarySync
import de.citrinscore.sync.supports
import de.citrinscore.ui.components.IconAction
import de.citrinscore.ui.components.IconBadgeSize
import de.citrinscore.ui.components.Tile
import de.citrinscore.ui.components.VerticalScrollIndicator
import de.citrinscore.ui.theme.ContentWidth
import de.citrinscore.ui.theme.Spacing
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    library: PlatformFile,
    onSettingsChange: (AppSettings, onFailure: () -> Unit) -> Unit,
    onLibraryMove: (LibraryTarget, onResult: (LibraryMoveResult) -> Unit) -> Unit,
    onOpenAbout: () -> Unit,
    onClose: () -> Unit,
) {
    val strings = LocalStrings.current
    var libraryError by remember { mutableStateOf(false) }
    var saveFailed by remember { mutableStateOf(false) }
    var pendingMove by remember { mutableStateOf<LibraryTarget?>(null) }
    var moving by remember { mutableStateOf(false) }
    var moveResult by remember { mutableStateOf<LibraryMoveResult?>(null) }
    val sync = librarySync()
    val currentTarget = if (settings.library is LibraryLocation.App) LibraryTarget.App else LibraryTarget.Folder(library)
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val change = { updated: AppSettings -> onSettingsChange(updated) { saveFailed = true } }

    LaunchedEffect(moveResult) {
        moveResult?.let { result ->
            strings.message(result)?.let { snackbarHostState.showSnackbar(it) }
            moveResult = null
        }
    }

    LaunchedEffect(saveFailed) {
        if (saveFailed) {
            snackbarHostState.showSnackbar(strings.settingsSaveFailed)
            saveFailed = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                navigationIcon = {
                    IconAction(Res.drawable.ic_arrow_back, strings.back, onClick = onClose)
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(Spacing.Page),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = ContentWidth.Regular).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.Section)) {
                    SettingsSection(strings.settingsLanguage) {
                        LanguageOptions(settings.language, onSelect = { change(settings.copy(language = it)) })
                    }
                    SettingsSection(strings.settingsTheme) {
                        ThemeOptions(settings.theme, onSelect = { change(settings.copy(theme = it)) })
                    }
                    SettingsSection(strings.settingsLibrary) {
                        LibraryLocationOptions(
                            selected = currentTarget,
                            showError = libraryError,
                            onSelect = { target ->
                                libraryError = false
                                if (target != currentTarget) pendingMove = target
                            },
                            onError = { libraryError = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !moving,
                        )
                    }
                    SettingsSection(strings.settingsSync) {
                        SyncSettings(
                            remote = settings.git,
                            library = library,
                            sync = sync,
                            available = sync.supports(settings.library),
                            onRemoteChange = { remote -> change(settings.copy(git = remote)) },
                        )
                    }
                    SettingsSection(strings.settingsAbout) {
                        AboutTile(onOpenAbout, Modifier.fillMaxWidth())
                    }
                }
            }
            VerticalScrollIndicator(scrollState, Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = Spacing.ExtraSmall))
        }
    }

    pendingMove?.let { target ->
        MoveLibraryDialog(
            target = when (target) {
                LibraryTarget.App -> strings.libraryInApp
                is LibraryTarget.Folder -> target.directory.name
            },
            stopsSync = settings.git != null && !(target == LibraryTarget.App || sync.syncsFolders),
            onConfirm = {
                pendingMove = null
                moving = true
                onLibraryMove(target) { result ->
                    moving = false
                    moveResult = result
                }
            },
            onDismiss = { pendingMove = null },
        )
    }
    if (moving) MovingLibraryDialog()
}

private fun Strings.message(result: LibraryMoveResult) = when (result) {
    LibraryMoveResult.Moved -> null
    LibraryMoveResult.SourceKept -> libraryMoveSourceKept
    LibraryMoveResult.TargetOccupied -> libraryMoveOccupied
    LibraryMoveResult.Failed -> libraryMoveFailed
}

@Composable
internal fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.Medium)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun AboutTile(onClick: () -> Unit, modifier: Modifier) {
    val strings = LocalStrings.current
    Tile(modifier, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.Page)) {
            Image(painterResource(Res.drawable.app_icon), contentDescription = null, modifier = Modifier.size(IconBadgeSize.Large))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                Text(AppInfo.NAME, style = MaterialTheme.typography.headlineSmall)
                Text(strings.appVersion(AppInfo.VERSION), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(strings.aboutTitle, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

