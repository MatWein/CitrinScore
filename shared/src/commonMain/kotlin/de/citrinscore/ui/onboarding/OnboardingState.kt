package de.citrinscore.ui.onboarding

import de.citrinscore.i18n.AppLanguage
import de.citrinscore.ui.components.isLastStep
import io.github.vinceglb.filekit.PlatformFile

enum class OnboardingStep { Welcome, Language, Library }

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val language: AppLanguage = AppLanguage.fromSystem(),
    val library: PlatformFile? = null,
    val libraryError: Boolean = false,
    val isCompleting: Boolean = false,
) {
    val canAdvance get() = !isCompleting && (!step.isLastStep() || library != null)
}
