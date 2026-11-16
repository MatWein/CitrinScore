package de.citrinscore.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.citrinscore.library.LibraryRepository
import de.citrinscore.ui.importer.ImportScreen
import de.citrinscore.ui.viewer.ScoreViewModel
import de.citrinscore.ui.viewer.ScoreViewerScreen
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.serialization.Serializable

@Serializable
internal data object LibraryRoute

@Serializable
internal data object ImportRoute

@Serializable
internal data object ScoreRoute

@Composable
fun LibraryNavigation(library: PlatformFile) {
    val repository = remember { LibraryRepository() }
    val libraryViewModel = viewModel(key = library.path) { LibraryViewModel(library, repository) }
    val navController = rememberNavController()

    NavHost(navController, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(
                viewModel = libraryViewModel,
                onImport = { navController.navigate(ImportRoute) },
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
                ScoreViewerScreen(viewModel, onClose = { close() })
            }
        }
    }
}
