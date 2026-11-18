package de.citrinscore.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.settings.AppSettings
import de.citrinscore.library.LibraryMoveResult
import de.citrinscore.library.moveLibrary
import de.citrinscore.settings.LibraryLocation
import de.citrinscore.settings.LibraryTarget
import de.citrinscore.settings.SettingsRepository
import de.citrinscore.sync.librarySync
import de.citrinscore.sync.supports
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

    fun switchLibrary(directory: PlatformFile, onFailure: () -> Unit) {
        val unavailable = _state.value as? AppState.LibraryUnavailable ?: return
        viewModelScope.launch {
            val settings = attempt {
                unavailable.settings.copy(library = LibraryLocation.Folder.of(directory)).also { settingsRepository.save(it) }
            }
            if (settings == null) onFailure() else _state.value = AppState.Ready(settings, directory)
        }
    }

    fun moveLibrary(target: LibraryTarget, onResult: (LibraryMoveResult) -> Unit) {
        val current = _state.value as? AppState.Ready ?: return
        viewModelScope.launch {
            val destination = attempt { target.location() to target.directory() } ?: return@launch onResult(LibraryMoveResult.Failed)
            val (location, directory) = destination
            val sync = librarySync()
            val synchronized = sync.supports(location)
            val settings = current.settings.copy(library = location, git = current.settings.git.takeIf { synchronized })
            val result = moveLibrary(
                source = current.library,
                target = directory,
                discarded = if (synchronized) emptySet() else sync.metadata,
                commit = { settingsRepository.save(settings) },
            )
            if (result == LibraryMoveResult.Moved || result == LibraryMoveResult.SourceKept) _state.value = AppState.Ready(settings, directory)
            onResult(result)
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
        val described = (settings.library as? LibraryLocation.Folder)?.let { settings.copy(library = it.describing(library)) } ?: settings
        if (described != settings) attempt { settingsRepository.save(described) }
        AppState.Ready(described, library)
    }
}
