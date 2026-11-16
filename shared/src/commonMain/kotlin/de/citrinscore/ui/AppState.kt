package de.citrinscore.ui

import de.citrinscore.settings.AppSettings
import io.github.vinceglb.filekit.PlatformFile

sealed interface AppState {
    data object Loading : AppState
    data object Onboarding : AppState
    data class LibraryUnavailable(val settings: AppSettings) : AppState
    data class Ready(val settings: AppSettings, val library: PlatformFile) : AppState
}
