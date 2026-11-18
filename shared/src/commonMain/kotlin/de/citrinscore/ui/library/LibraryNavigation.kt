package de.citrinscore.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import de.citrinscore.library.LibraryMoveResult
import de.citrinscore.library.LibraryRepository
import de.citrinscore.settings.AppSettings
import de.citrinscore.settings.LibraryTarget
import de.citrinscore.ui.editor.EditScoreScreen
import de.citrinscore.ui.importer.ImportScreen
import de.citrinscore.sync.librarySync
import de.citrinscore.sync.supports
import de.citrinscore.ui.settings.AboutScreen
import de.citrinscore.ui.settings.LicensesScreen
import de.citrinscore.ui.settings.SettingsScreen
import de.citrinscore.ui.sync.SyncDialog
import de.citrinscore.ui.sync.SyncViewModel
import de.citrinscore.ui.viewer.ScoreViewModel
import de.citrinscore.ui.viewer.ScoreViewerScreen
import de.citrinscore.ui.viewer.annotations.AnnotationViewModel
import de.citrinscore.ui.viewer.audio.ScoreAudioViewModel
import de.citrinscore.ui.viewer.playback.PlaybackViewModel
import de.citrinscore.ui.viewer.files.ScoreFilesViewModel
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

@Serializable
internal data object EditRoute

@Serializable
internal data object AboutRoute

@Serializable
internal data object LicensesRoute

@Composable
fun LibraryNavigation(
    settings: AppSettings,
    library: PlatformFile,
    onSettingsChange: (AppSettings, onFailure: () -> Unit) -> Unit,
    onLibraryMove: (LibraryTarget, onResult: (LibraryMoveResult) -> Unit) -> Unit,
) {
    val repository = remember { LibraryRepository() }
    val libraryViewModel = viewModel(key = library.path) { LibraryViewModel(library, repository) }
    val navController = rememberNavController()
    val syncViewModel = librarySync()?.takeIf { it.supports(settings.library) }?.let { sync -> viewModel(key = "${library.path}/sync") { SyncViewModel(library, sync) } }

    NavHost(navController, startDestination = LibraryRoute) {
        composable<LibraryRoute> {
            LibraryScreen(
                viewModel = libraryViewModel,
                sort = settings.librarySort,
                onSortChange = { sort ->
                    onSettingsChange(settings.copy(librarySort = sort)) { libraryViewModel.report(LibraryError.SaveSettings) }
                },
                onImport = { navController.navigate(ImportRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenScore = { score ->
                    libraryViewModel.select(score)
                    navController.navigate(ScoreRoute)
                },
                onEditScore = { score ->
                    libraryViewModel.select(score)
                    navController.navigate(EditRoute)
                },
                onSync = syncViewModel?.let { sync -> settings.git?.let { remote -> { sync.start(remote, libraryViewModel::reload) } } },
            )
            syncViewModel?.let { sync ->
                val syncState by sync.state.collectAsStateWithLifecycle()
                SyncDialog(syncState, sync)
            }
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
        composable<EditRoute> {
            val score = libraryViewModel.selectedScore
            if (score == null) {
                LaunchedEffect(Unit) { navController.popBackStack() }
            } else {
                EditScoreScreen(
                    folder = libraryViewModel.currentFolder,
                    score = score,
                    repository = repository,
                    onClose = { navController.popBackStack() },
                    onSaved = {
                        libraryViewModel.invalidateThumbnail(score)
                        libraryViewModel.refresh()
                        navController.popBackStack()
                    },
                )
            }
        }
        composable<SettingsRoute> {
            SettingsScreen(
                settings = settings,
                library = library,
                onSettingsChange = onSettingsChange,
                onLibraryMove = onLibraryMove,
                onOpenAbout = { navController.navigate(AboutRoute) },
                onClose = { navController.popBackStack() },
            )
        }
        composable<AboutRoute> {
            AboutScreen(
                onOpenLicenses = { navController.navigate(LicensesRoute) },
                onClose = { navController.popBackStack() },
            )
        }
        composable<LicensesRoute> {
            LicensesScreen(onClose = { navController.popBackStack() })
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
                val audioViewModel = viewModel(key = "${score.path}/audio") { ScoreAudioViewModel(viewModel.part, repository) }
                val annotationViewModel = viewModel(key = "${score.path}/annotations") { AnnotationViewModel(viewModel.part, repository, viewModel.anchors) }
                val filesViewModel = viewModel(key = "${score.path}/files") { ScoreFilesViewModel(viewModel.part, repository) }
                val playbackViewModel = viewModel(key = "${score.path}/playback") { PlaybackViewModel(viewModel.part, viewModel::performance, viewModel::writtenPitches) }
                ScoreViewerScreen(viewModel, audioViewModel, annotationViewModel, filesViewModel, playbackViewModel, onClose = { close() })
            }
        }
    }
}
