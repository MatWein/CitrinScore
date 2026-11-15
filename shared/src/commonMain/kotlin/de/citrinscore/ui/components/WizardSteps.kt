package de.citrinscore.ui.components

import kotlin.enums.enumEntries

inline fun <reified E : Enum<E>> E.isLastStep() = ordinal == enumEntries<E>().lastIndex

inline fun <reified E : Enum<E>> E.nextStep(): E = enumEntries<E>().getOrElse(ordinal + 1) { this }

inline fun <reified E : Enum<E>> E.previousStep(): E = enumEntries<E>().getOrElse(ordinal - 1) { this }
