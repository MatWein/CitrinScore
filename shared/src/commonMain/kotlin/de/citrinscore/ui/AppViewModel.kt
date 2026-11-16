package de.citrinscore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.settings.AppSettings
import de.citrinscore.settings.LibraryReference
import de.citrinscore.settings.SettingsRepository
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.exists
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = attempt { settingsRepository.load() }
                ?.let { settings -> readyState(settings) ?: AppState.LibraryUnavailable(settings) }
                ?: AppState.Onboarding
        }
    }

    fun completeOnboarding(settings: AppSettings, onFailure: () -> Unit) {
        viewModelScope.launch {
            val ready = attempt {
                settingsRepository.save(settings)
                readyState(settings)
            }
            if (ready == null) onFailure() else _state.value = ready
        }
    }

    fun updateSettings(settings: AppSettings, onFailure: () -> Unit) {
        val current = _state.value as? AppState.Ready ?: return
        _state.value = current.copy(settings = settings)
        viewModelScope.launch { attempt { settingsRepository.save(settings) } ?: onFailure() }
    }

    fun changeLibrary(directory: PlatformFile, onFailure: () -> Unit) {
        val current = when (val state = _state.value) {
            is AppState.Ready -> state.settings
            is AppState.LibraryUnavailable -> state.settings
            else -> return
        }
        viewModelScope.launch {
            val settings = attempt {
                current.copy(library = LibraryReference.of(directory)).also { settingsRepository.save(it) }
            }
            if (settings == null) onFailure() else _state.value = AppState.Ready(settings, directory)
        }
    }

    fun retryLibrary(onFailure: () -> Unit) {
        val unavailable = _state.value as? AppState.LibraryUnavailable ?: return
        viewModelScope.launch {
            val ready = readyState(unavailable.settings)
            if (ready == null) onFailure() else _state.value = ready
        }
    }

    private suspend fun readyState(settings: AppSettings): AppState.Ready? = attempt {
        val library = settings.library.resolve().takeIf { it.exists() } ?: return@attempt null
        val described = settings.copy(library = settings.library.describing(library))
        if (described != settings) attempt { settingsRepository.save(described) }
        AppState.Ready(described, library)
    }
}
