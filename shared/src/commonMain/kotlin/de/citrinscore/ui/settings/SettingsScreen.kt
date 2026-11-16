package de.citrinscore.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.resources.Res
import de.citrinscore.resources.ic_arrow_back
import de.citrinscore.settings.AppSettings
import de.citrinscore.ui.components.ContentMaxWidth
import de.citrinscore.ui.components.VerticalScrollIndicator
import io.github.vinceglb.filekit.PlatformFile
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    library: PlatformFile,
    onSettingsChange: (AppSettings) -> Unit,
    onLibraryChange: (directory: PlatformFile, onFailure: () -> Unit) -> Unit,
    onClose: () -> Unit,
) {
    val strings = LocalStrings.current
    var libraryError by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(strings.settings) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(scrollState).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(Modifier.widthIn(max = ContentMaxWidth).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(32.dp)) {
                    SettingsSection(strings.settingsLanguage) {
                        LanguageOptions(settings.language, onSelect = { onSettingsChange(settings.copy(language = it)) })
                    }
                    SettingsSection(strings.settingsTheme) {
                        ThemeOptions(settings.theme, onSelect = { onSettingsChange(settings.copy(theme = it)) })
                    }
                    SettingsSection(strings.settingsLibrary) {
                        LibraryFolderTile(
                            library = library,
                            showError = libraryError,
                            onSelect = { directory ->
                                libraryError = false
                                onLibraryChange(directory) { libraryError = true }
                            },
                            onError = { libraryError = true },
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            VerticalScrollIndicator(scrollState, Modifier.align(Alignment.CenterEnd).fillMaxHeight().padding(end = 4.dp))
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        content()
    }
}
