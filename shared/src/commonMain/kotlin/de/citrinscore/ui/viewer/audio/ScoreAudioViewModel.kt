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
import io.github.vinceglb.filekit.path
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.TimeSource

private val PositionInterval = 200.milliseconds
private val logger = LoggerFactory.getLogger(ScoreAudioViewModel::class.java)
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
            reporting(AudioError.Save) {
                val attachments = repository.attachments(score, AttachmentKind.Audio)
                _state.update { it.copy(attachments = attachments) }
            }
        }
    }

    fun load(attachment: Attachment) {
        logger.info("Opening audio {} of {}", attachment.path, score.path)
        releasePlayer()
        viewModelScope.launch {
            reporting(AudioError.Playback) {
                val opened = openAudioPlayer(CitScore.resolve(score, attachment.path))
                logger.info("Opened audio with duration {}", opened.duration)
                player = opened
                _state.update { it.copy(player = PlayerState(attachment, opened.duration, Duration.ZERO, isPlaying = false)) }
            }
        }
    }

    fun play() {
        val current = player ?: return
        logger.info("Playing audio at {}", current.position)
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
            logger.info("Pausing audio at {}", it.position)
            it.pause()
            publishPosition(it)
        }
    }

    fun stop() {
        player?.let {
            logger.info("Stopping audio")
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
            logger.info("Microphone available: {}", available)
            _state.update { it.copy(microphoneAvailable = available) }
        }
    }

    fun startRecording() {
        if (recording != null) return
        logger.info("Starting recording to {}", recordingFile.path)
        closePlayer()
        viewModelScope.launch {
            reporting(AudioError.Recording) {
                recording = startAudioRecording(recordingFile)
                _state.update { it.copy(recordingStart = TimeSource.Monotonic.markNow()) }
            }
        }
    }

    fun stopRecording() {
        val length = _state.value.recordingStart?.elapsedNow() ?: return
        val active = takeRecording() ?: return
        logger.info("Stopping recording after {}", length)
        viewModelScope.launch {
            reporting(AudioError.Save) {
                active.finish()
                _state.update { it.copy(finishedRecording = length) }
            }
        }
    }

    fun saveRecording(name: String) {
        _state.update { it.copy(finishedRecording = null) }
        viewModelScope.launch { storeRecording(name) }
    }

    fun discardRecording() {
        logger.info("Discarding recording")
        _state.update { it.copy(finishedRecording = null) }
        viewModelScope.launch { recordingFile.delete(mustExist = false) }
    }

    fun saveActiveRecording(name: String) {
        val active = takeRecording() ?: return
        viewModelScope.launch { storeRecording(name, finishing = active) }
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

    fun export(attachment: Attachment, target: PlatformFile) {
        viewModelScope.launch { reporting(AudioError.Save) { repository.exportAttachment(score, attachment, target) } }
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

    private fun takeRecording(): AudioRecording? {
        val active = recording ?: return null
        recording = null
        _state.update { it.copy(recordingStart = null) }
        return active
    }

    private suspend fun storeRecording(name: String, finishing: AudioRecording? = null) {
        reporting(AudioError.Save) {
            finishing?.finish()
            append(repository.addAttachment(score, AttachmentKind.Audio, recordingFile, name))
        }
        recordingFile.delete(mustExist = false)
    }

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
