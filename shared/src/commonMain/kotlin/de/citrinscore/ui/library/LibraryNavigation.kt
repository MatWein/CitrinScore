package de.citrinscore.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.citrinscore.library.LibraryRepository
import de.citrinscore.settings.AppSettings
import de.citrinscore.ui.importer.ImportScreen
import de.citrinscore.ui.settings.SettingsScreen
import de.citrinscore.ui.viewer.ScoreViewModel
import de.citrinscore.ui.viewer.ScoreViewerScreen
import de.citrinscore.ui.viewer.audio.ScoreAudioViewModel
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.serialization.Serializable

@Serializable
internal data object LibraryRoute

@Serializable
internal data object ImportRoute

@Serializable
internal data object ScoreRoute

@Serializable
internal data object SettingsRoute

@Composable
fun LibraryNavigation(
    settings: AppSettings,
    library: PlatformFile,
    onSettingsChange: (AppSettings) -> Unit,
    onLibraryChange: (directory: PlatformFile, onFailure: () -> Unit) -> Unit,
) {
    val repository = remember { LibraryRepository() }
    val libraryViewModel = viewModel(key = library.path) { LibraryViewModel(library, repository) }
    val navController = rememberNavController()

    NavHost(navController, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(
                viewModel = libraryViewModel,
                onImport = { navController.navigate(ImportRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenScore = { score ->
                    libraryViewModel.select(score)
                    navController.navigate(ScoreRoute)
                },
            )
        }
        composable<ImportRoute> {
            ImportScreen(
                folder = libraryViewModel.currentFolder,
                repository = repository,
                onClose = { navController.popBackStack() },
                onImported = {
                    libraryViewModel.refresh()
                    navController.popBackStack()
                },
            )
        }
        composable<SettingsRoute> {
            SettingsScreen(
                settings = settings,
                library = library,
                onSettingsChange = onSettingsChange,
                onLibraryChange = onLibraryChange,
                onClose = { navController.popBackStack() },
            )
        }
        composable<ScoreRoute> {
            val score = libraryViewModel.selectedScore
            val close = {
                libraryViewModel.refresh()
                navController.popBackStack()
            }
            if (score == null) {
                LaunchedEffect(Unit) { close() }
            } else {
                val viewModel = viewModel(key = score.path) { ScoreViewModel(score, repository) }
                val audioViewModel = viewModel(key = "${score.path}/audio") { ScoreAudioViewModel(score, repository) }
                ScoreViewerScreen(viewModel, audioViewModel, onClose = { close() })
            }
        }
    }
}
