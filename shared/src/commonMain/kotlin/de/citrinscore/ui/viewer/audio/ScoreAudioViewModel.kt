package de.citrinscore.ui.viewer.audio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.LibraryRepository
import de.citrinscore.score.Attachment
import de.citrinscore.score.AttachmentKind
import de.citrinscore.score.CitScore
import de.citrinscore.score.audio.AudioPlayer
import de.citrinscore.score.audio.AudioRecording
import de.citrinscore.score.audio.isMicrophoneAvailable
import de.citrinscore.score.audio.openAudioPlayer
import de.citrinscore.score.audio.startAudioRecording
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.nameWithoutExtension
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val PositionInterval = 200.milliseconds
private const val RECORDING_FILE = "recording.${CitScore.RECORDING_EXTENSION}"

class ScoreAudioViewModel(private val score: PlatformFile, private val repository: LibraryRepository) : ViewModel() {

    private val _state = MutableStateFlow(AudioState())
    val state: StateFlow<AudioState> = _state.asStateFlow()

    private var player: AudioPlayer? = null
    private var positionJob: Job? = null
    private var recording: AudioRecording? = null
    private val recordingFile get() = FileKit.cacheDir / RECORDING_FILE

    init {
        viewModelScope.launch {
            reporting(AudioError.Save) { _state.update { it.copy(attachments = repository.attachments(score, AttachmentKind.Audio)) } }
        }
    }

    fun load(attachment: Attachment) {
        releasePlayer()
        viewModelScope.launch {
            reporting(AudioError.Playback) {
                val opened = openAudioPlayer(CitScore.resolve(score, attachment.path))
                player = opened
                _state.update { it.copy(player = PlayerState(attachment, opened.duration, Duration.ZERO, isPlaying = false)) }
            }
        }
    }

    fun play() {
        val current = player ?: return
        current.play()
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (isActive) {
                publishPosition(current)
                if (!current.isPlaying) break
                delay(PositionInterval)
            }
        }
    }

    fun pause() {
        player?.let {
            it.pause()
            publishPosition(it)
        }
    }

    fun stop() {
        player?.let {
            it.pause()
            it.seekTo(Duration.ZERO)
            publishPosition(it)
        }
    }

    fun seekTo(position: Duration) {
        player?.let {
            it.seekTo(position)
            publishPosition(it)
        }
    }

    fun closePlayer() {
        releasePlayer()
        _state.update { it.copy(player = null) }
    }

    fun addFiles(files: List<PlatformFile>) {
        viewModelScope.launch {
            reporting(AudioError.Save) {
                files.forEach { file -> append(repository.addAttachment(score, AttachmentKind.Audio, file, file.nameWithoutExtension)) }
            }
        }
    }

    fun checkMicrophone() {
        if (_state.value.microphoneAvailable != null) return
        viewModelScope.launch {
            val available = isMicrophoneAvailable()
            _state.update { it.copy(microphoneAvailable = available) }
        }
    }

    fun startRecording() {
        if (recording != null) return
        viewModelScope.launch {
            reporting(AudioError.Recording) {
                recording = startAudioRecording(recordingFile)
                _state.update { it.copy(recordingStart = TimeSource.Monotonic.markNow()) }
            }
        }
    }

    fun finishRecording(name: String) {
        val active = recording ?: return
        recording = null
        _state.update { it.copy(recordingStart = null) }
        viewModelScope.launch {
            reporting(AudioError.Save) {
                active.finish()
                append(repository.addAttachment(score, AttachmentKind.Audio, recordingFile, name))
            }
            recordingFile.delete(mustExist = false)
        }
    }

    fun discardRecording() {
        recording?.cancel()
        recording = null
        _state.update { it.copy(recordingStart = null) }
    }

    fun rename(attachment: Attachment, name: String) {
        viewModelScope.launch {
            reporting(AudioError.Save) {
                repository.renameAttachment(score, AttachmentKind.Audio, attachment, name)
                _state.update { state ->
                    val renamed = attachment.copy(name = name)
                    state.copy(
                        attachments = state.attachments.map { if (it.path == attachment.path) renamed else it },
                        player = state.player?.let { if (it.attachment.path == attachment.path) it.copy(attachment = renamed) else it },
                    )
                }
            }
        }
    }

    fun delete(attachment: Attachment) {
        if (_state.value.player?.attachment?.path == attachment.path) closePlayer()
        viewModelScope.launch {
            reporting(AudioError.Save) {
                repository.deleteAttachment(score, AttachmentKind.Audio, attachment)
                _state.update { state -> state.copy(attachments = state.attachments.filterNot { it.path == attachment.path }) }
            }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    private fun append(attachment: Attachment) = _state.update { it.copy(attachments = it.attachments + attachment) }

    private fun publishPosition(current: AudioPlayer) = _state.update { state ->
        state.copy(player = state.player?.copy(position = current.position, isPlaying = current.isPlaying))
    }

    private fun releasePlayer() {
        positionJob?.cancel()
        player?.close()
        player = null
    }

    private inline fun reporting(error: AudioError, block: () -> Unit) {
        attempt(block) ?: _state.update { it.copy(error = error) }
    }

    override fun onCleared() {
        releasePlayer()
        recording?.cancel()
    }
}
