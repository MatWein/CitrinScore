package de.citrinscore.ui.viewer.playback

import de.citrinscore.score.playback.Instrument

enum class PlaybackMode { Listen, PlayAlong }

enum class PlaybackError { Score, Synthesizer, Microphone }

data class PlaybackState(
    val isOpen: Boolean = false,
    val mode: PlaybackMode = PlaybackMode.Listen,
    val isLoading: Boolean = false,
    val isRunning: Boolean = false,
    val tempo: Float = 1f,
    val instrument: Instrument? = null,
    val microphoneAvailable: Boolean? = null,
    val error: PlaybackError? = null,
)

data class PlaybackCursor(val notes: List<String>, val highlighted: List<String>, val follow: Boolean)
