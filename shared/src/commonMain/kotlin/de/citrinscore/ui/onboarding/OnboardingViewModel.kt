package de.citrinscore.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.i18n.AppLanguage
import de.citrinscore.settings.AppSettings
import de.citrinscore.settings.LibraryTarget
import de.citrinscore.settings.ThemeMode
import de.citrinscore.ui.components.isLastStep
import de.citrinscore.ui.components.nextStep
import de.citrinscore.ui.components.previousStep
import de.citrinscore.util.attempt
import de.citrinscore.util.logChanges
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(OnboardingViewModel::class.java)

class OnboardingViewModel : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    init {
        viewModelScope.logChanges(_state.map { it.step }, logger, "Onboarding step {}")
    }

    fun selectLanguage(language: AppLanguage) = _state.update { it.copy(language = language) }

    fun selectTheme(theme: ThemeMode) = _state.update { it.copy(theme = theme) }

    fun selectLibrary(target: LibraryTarget) = _state.update { it.copy(library = target, libraryError = false) }

    fun reportLibraryError() = _state.update { it.copy(libraryError = true) }

    fun back() = _state.update { it.copy(step = it.step.previousStep()) }

    fun next(onFinished: (AppSettings, onFailure: () -> Unit) -> Unit) {
        val current = _state.value
        if (!current.canAdvance) return
        if (!current.step.isLastStep()) {
            _state.update { it.copy(step = it.step.nextStep()) }
            return
        }
        val library = current.library ?: return
        _state.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            val location = attempt { library.location() } ?: return@launch failCompletion()
            onFinished(AppSettings(current.language, location, current.theme), ::failCompletion)
        }
    }

    private fun failCompletion() = _state.update { it.copy(isCompleting = false, libraryError = true) }
}
