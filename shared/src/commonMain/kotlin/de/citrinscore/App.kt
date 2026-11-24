package de.citrinscore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.settings.AppSettings
import de.citrinscore.settings.FileSettingsRepository
import de.citrinscore.ui.AppState
import de.citrinscore.ui.AppViewModel
import de.citrinscore.ui.LocalWindowTitle
import de.citrinscore.ui.library.LibraryNavigation
import de.citrinscore.ui.library.LibraryUnavailableScreen
import de.citrinscore.ui.onboarding.OnboardingScreen
import de.citrinscore.ui.theme.CitrinScreen
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir

@Composable
fun App(onWindowTitleChange: (String?) -> Unit = {}) {
    val viewModel = viewModel { AppViewModel(FileSettingsRepository(FileKit.filesDir)) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalWindowTitle provides onWindowTitleChange) { AppContent(state, viewModel) }
}

@Composable
private fun AppContent(state: AppState, viewModel: AppViewModel) {
    AnimatedContent(
        targetState = state,
        contentKey = { it::class },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
    ) { current ->
        when (current) {
            AppState.Loading -> Unit
            AppState.Onboarding -> OnboardingScreen(onFinished = viewModel::completeOnboarding)
            is AppState.LibraryUnavailable -> ConfiguredScreen(current.settings) {
                LibraryUnavailableScreen(
                    location = current.settings.library,
                    onRetry = viewModel::retryLibrary,
                    onChooseFolder = viewModel::switchLibrary,
                )
            }
            is AppState.Ready -> ConfiguredScreen(current.settings) {
                LibraryNavigation(
                    settings = current.settings,
                    library = current.library,
                    onSettingsChange = viewModel::updateSettings,
                    onLibraryMove = viewModel::moveLibrary,
                )
            }
        }
    }
}

@Composable
private fun ConfiguredScreen(settings: AppSettings, content: @Composable () -> Unit) {
    CitrinScreen(settings.theme) {
        CompositionLocalProvider(LocalStrings provides settings.language.strings, content = content)
    }
}
