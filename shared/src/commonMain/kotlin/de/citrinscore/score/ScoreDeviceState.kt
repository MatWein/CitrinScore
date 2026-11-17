package de.citrinscore.score

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ScoreDeviceState(
    val lastOpenedAt: Instant? = null,
    val currentPart: String? = null,
    val views: Map<String, ScoreViewState> = emptyMap(),
)

val ScoreManifest.deviceState
    get() = ScoreDeviceState(lastOpenedAt, currentPart, parts.mapNotNull { part -> part.view?.let { part.directory to it } }.toMap())

val ScoreManifest.shared
    get() = copy(lastOpenedAt = null, currentPart = null, parts = parts.map { it.copy(view = null) })

fun ScoreManifest.withDeviceState(state: ScoreDeviceState) = copy(
    lastOpenedAt = state.lastOpenedAt ?: lastOpenedAt,
    currentPart = state.currentPart ?: currentPart,
    parts = parts.map { part -> state.views[part.directory]?.let { part.copy(view = it) } ?: part },
)
