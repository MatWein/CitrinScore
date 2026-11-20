package de.citrinscore.score

import kotlinx.serialization.Serializable
import kotlin.time.Instant

@Serializable
data class ScoreDeviceState(
    val lastOpenedAt: Instant? = null,
    val currentPart: String? = null,
    val views: Map<String, ScoreViewState> = emptyMap(),
    val engravedViews: Map<String, ScoreViewState> = emptyMap(),
    val displays: Map<String, PartDisplay> = emptyMap(),
    val playback: Map<String, PlaybackPreferences> = emptyMap(),
)

val ScoreManifest.deviceState
    get() = ScoreDeviceState(
        lastOpenedAt = lastOpenedAt,
        currentPart = currentPart,
        views = parts.mapNotNull { part -> part.view?.let { part.directory to it } }.toMap(),
        engravedViews = parts.mapNotNull { part -> part.engravedView?.let { part.directory to it } }.toMap(),
        displays = parts.mapNotNull { part -> part.display?.let { part.directory to it } }.toMap(),
        playback = parts.mapNotNull { part -> part.playback?.let { part.directory to it } }.toMap(),
    )

val ScoreManifest.shared
    get() = copy(lastOpenedAt = null, currentPart = null, parts = parts.map { it.copy(view = null, engravedView = null, display = null, playback = null) })

fun ScoreManifest.withDeviceState(state: ScoreDeviceState) = copy(
    lastOpenedAt = state.lastOpenedAt ?: lastOpenedAt,
    currentPart = state.currentPart ?: currentPart,
    parts = parts.map { part ->
        part.copy(
            view = state.views[part.directory] ?: part.view,
            engravedView = state.engravedViews[part.directory] ?: part.engravedView,
            display = state.displays[part.directory] ?: part.display,
            playback = state.playback[part.directory] ?: part.playback,
        )
    },
)
