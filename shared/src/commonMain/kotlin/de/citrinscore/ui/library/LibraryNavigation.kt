package de.citrinscore.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.citrinscore.library.LibraryRepository
import de.citrinscore.ui.importer.ImportScreen
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.path
import kotlinx.serialization.Serializable

@Serializable
internal data object LibraryRoute

@Serializable
internal data object ImportRoute

@Composable
fun LibraryNavigation(library: PlatformFile) {
    val repository = remember { LibraryRepository() }
    val libraryViewModel = viewModel(key = library.path) { LibraryViewModel(library, repository) }
    val navController = rememberNavController()

    NavHost(navController, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(libraryViewModel, onImport = { navController.navigate(ImportRoute) })
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
    }
}
