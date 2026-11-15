package de.citrinscore

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.settings.FileSettingsRepository
import de.citrinscore.ui.AppState
import de.citrinscore.ui.AppViewModel
import de.citrinscore.ui.library.LibraryScreen
import de.citrinscore.ui.onboarding.OnboardingScreen
import de.citrinscore.ui.theme.CitrinTheme
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.filesDir

@Composable
fun App() {
    val viewModel = viewModel { AppViewModel(FileSettingsRepository(FileKit.filesDir)) }
    val state by viewModel.state.collectAsStateWithLifecycle()

    CitrinTheme {
        Surface(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state,
                contentKey = { it::class },
                transitionSpec = { fadeIn() togetherWith fadeOut() },
            ) { current ->
                when (current) {
                    AppState.Loading -> Unit
                    AppState.Onboarding -> OnboardingScreen(onFinished = viewModel::completeOnboarding)
                    is AppState.Ready -> CompositionLocalProvider(LocalStrings provides current.settings.language.strings) {
                        LibraryScreen(current.settings)
                    }
                }
            }
        }
    }
}
