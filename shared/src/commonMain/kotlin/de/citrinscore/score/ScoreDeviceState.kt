package de.citrinscore.score

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ScoreDeviceState(
    val lastOpenedAt: Instant? = null,
    val currentPart: String? = null,
    val views: Map<String, ScoreViewState> = emptyMap(),
    val playback: Map<String, PlaybackPreferences> = emptyMap(),
)

val ScoreManifest.deviceState
    get() = ScoreDeviceState(
        lastOpenedAt = lastOpenedAt,
        currentPart = currentPart,
        views = parts.mapNotNull { part -> part.view?.let { part.directory to it } }.toMap(),
        playback = parts.mapNotNull { part -> part.playback?.let { part.directory to it } }.toMap(),
    )

val ScoreManifest.shared
    get() = copy(lastOpenedAt = null, currentPart = null, parts = parts.map { it.copy(view = null, playback = null) })

fun ScoreManifest.withDeviceState(state: ScoreDeviceState) = copy(
    lastOpenedAt = state.lastOpenedAt ?: lastOpenedAt,
    currentPart = state.currentPart ?: currentPart,
    parts = parts.map { part -> part.copy(view = state.views[part.directory] ?: part.view, playback = state.playback[part.directory] ?: part.playback) },
)
