package de.citrinscore.ui.onboarding

import de.citrinscore.i18n.AppLanguage
import de.citrinscore.settings.LibraryTarget
import de.citrinscore.settings.ThemeMode
import de.citrinscore.settings.appStorageSelectable
import de.citrinscore.ui.components.isLastStep

enum class OnboardingStep { Welcome, Language, Theme, Library }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val language: AppLanguage = AppLanguage.fromSystem(),
    val theme: ThemeMode = ThemeMode.System,
    val library: LibraryTarget? = LibraryTarget.App.takeIf { appStorageSelectable },
    val libraryError: Boolean = false,
    val isCompleting: Boolean = false,
) {
    val canAdvance get() = !isCompleting && (!step.isLastStep() || library != null)
}
