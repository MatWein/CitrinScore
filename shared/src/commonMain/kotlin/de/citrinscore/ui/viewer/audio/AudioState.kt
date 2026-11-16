package de.citrinscore.ui.viewer.audio

import de.citrinscore.score.AudioAttachment
import kotlin.time.Duration
import kotlin.time.TimeSource

enum class AudioError { Playback, Recording, Save }

data class PlayerState(
    val attachment: AudioAttachment,
    val duration: Duration,
    val position: Duration,
    val isPlaying: Boolean,
)

data class AudioState(
    val attachments: List<AudioAttachment> = emptyList(),
    val player: PlayerState? = null,
    val recordingStart: TimeSource.Monotonic.ValueTimeMark? = null,
    val microphoneAvailable: Boolean? = null,
    val error: AudioError? = null,
) {
    val nextRecordingNumber get() = attachments.size + 1
}
