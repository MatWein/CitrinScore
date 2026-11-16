package de.citrinscore.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.i18n.AppLanguage
import de.citrinscore.settings.AppSettings
import de.citrinscore.settings.LibraryReference
import de.citrinscore.settings.ThemeMode
import de.citrinscore.ui.components.isLastStep
import de.citrinscore.ui.components.nextStep
import de.citrinscore.ui.components.previousStep
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel : ViewModel() {

    private val _state = MutableStateFlow(OnboardingState())
    val state: StateFlow<OnboardingState> = _state.asStateFlow()

    fun selectLanguage(language: AppLanguage) = _state.update { it.copy(language = language) }

    fun selectTheme(theme: ThemeMode) = _state.update { it.copy(theme = theme) }

    fun selectLibrary(directory: PlatformFile) = _state.update { it.copy(library = directory, libraryError = false) }

    fun reportLibraryError() = _state.update { it.copy(libraryError = true) }

    fun back() = _state.update { it.copy(step = it.step.previousStep()) }

    fun next(onFinished: (AppSettings) -> Unit) {
        val current = _state.value
        if (!current.canAdvance) return
        if (!current.step.isLastStep()) {
            _state.update { it.copy(step = it.step.nextStep()) }
            return
        }
        val library = current.library ?: return
        _state.update { it.copy(isCompleting = true) }
        viewModelScope.launch {
            try {
                onFinished(AppSettings(current.language, LibraryReference.of(library), current.theme))
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(isCompleting = false, libraryError = true) }
            }
        }
    }
}
