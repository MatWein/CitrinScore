package de.citrinscore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.settings.AppSettings
import de.citrinscore.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AppViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _state = MutableStateFlow<AppState>(AppState.Loading)
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = settingsRepository.load()?.let(AppState::Ready) ?: AppState.Onboarding
        }
    }

    fun completeOnboarding(settings: AppSettings) {
        viewModelScope.launch {
            settingsRepository.save(settings)
            _state.value = AppState.Ready(settings)
        }
    }
}
