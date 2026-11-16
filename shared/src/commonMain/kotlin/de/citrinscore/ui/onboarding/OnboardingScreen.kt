package de.citrinscore.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import de.citrinscore.i18n.LocalStrings
import de.citrinscore.settings.AppSettings
import de.citrinscore.ui.components.WizardScaffold
import de.citrinscore.ui.components.isLastStep
import de.citrinscore.ui.theme.CitrinScreen

@Composable
fun OnboardingScreen(
    onFinished: (AppSettings, onFailure: () -> Unit) -> Unit,
    viewModel: OnboardingViewModel = viewModel { OnboardingViewModel() },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = state.language.strings

    CitrinScreen(state.theme) {
        CompositionLocalProvider(LocalStrings provides strings) {
            WizardScaffold(
                step = state.step,
                stepCount = OnboardingStep.entries.size,
                backLabel = strings.back.takeIf { state.step != OnboardingStep.entries.first() },
                onBack = viewModel::back,
                nextLabel = if (state.step.isLastStep()) strings.getStarted else strings.next,
                nextEnabled = state.canAdvance,
                onNext = { viewModel.next(onFinished) },
            ) { step ->
                when (step) {
                    OnboardingStep.Welcome -> WelcomeStep()
                    OnboardingStep.Language -> LanguageStep(state.language, viewModel::selectLanguage)
                    OnboardingStep.Theme -> ThemeStep(state.theme, viewModel::selectTheme)
                    OnboardingStep.Library -> LibraryStep(
                        library = state.library,
                        showError = state.libraryError,
                        onSelect = viewModel::selectLibrary,
                        onError = viewModel::reportLibraryError,
                    )
                }
            }
        }
    }
}
