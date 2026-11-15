package de.citrinscore.ui

import de.citrinscore.settings.AppSettings

sealed interface AppState {
    data object Loading : AppState
    data object Onboarding : AppState
    data class Ready(val settings: AppSettings) : AppState
}
