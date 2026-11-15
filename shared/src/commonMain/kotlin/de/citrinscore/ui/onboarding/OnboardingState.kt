package de.citrinscore.ui.onboarding

import de.citrinscore.i18n.AppLanguage
import io.github.vinceglb.filekit.PlatformFile

enum class OnboardingStep {
    Welcome,
    Language,
    Library;

    val number get() = ordinal + 1
    val isFirst get() = this == entries.first()
    val isLast get() = this == entries.last()
    fun next() = entries.getOrElse(ordinal + 1) { this }
    fun previous() = entries.getOrElse(ordinal - 1) { this }
}

data class OnboardingState(
    val step: OnboardingStep = OnboardingStep.Welcome,
    val language: AppLanguage = AppLanguage.fromSystem(),
    val library: PlatformFile? = null,
    val libraryError: Boolean = false,
    val isCompleting: Boolean = false,
) {
    val canAdvance get() = !isCompleting && (!step.isLast || library != null)
}
