package de.citrinscore.ui.viewer.playback

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.citrinscore.library.PartRef
import de.citrinscore.library.PerformedPart
import de.citrinscore.score.PlaybackPreferences
import de.citrinscore.score.audio.isMicrophoneAvailable
import de.citrinscore.score.audio.microphoneSamples
import de.citrinscore.score.musicxml.PerformanceEvent
import de.citrinscore.score.musicxml.ScorePerformance
import de.citrinscore.score.playback.Instrument
import de.citrinscore.score.playback.NoteListener
import de.citrinscore.score.playback.ScoreFollower
import de.citrinscore.score.playback.ScoreSynthesizer
import de.citrinscore.score.playback.openScoreSynthesizer
import de.citrinscore.util.attempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

private const val MICROPHONE_SAMPLE_RATE = 22050
private val logger = LoggerFactory.getLogger(PlaybackViewModel::class.java)
const val MIN_TEMPO = 0.25f
const val MAX_TEMPO = 2f

class PlaybackViewModel(
    private val performed: StateFlow<PerformedPart?>,
    private val loadPerformance: suspend (PartRef) -> ScorePerformance?,
    private val loadWrittenPitches: suspend (PartRef, Collection<String>) -> Map<String, Int>,
    private val loadPreferences: (PartRef) -> PlaybackPreferences?,
    private val storePreferences: (PartRef, PlaybackPreferences) -> Unit,
) : ViewModel() {

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _cursor = MutableStateFlow<PlaybackCursor?>(null)
    val cursor: StateFlow<PlaybackCursor?> = _cursor.asStateFlow()

    private var performance: ScorePerformance? = null
    private var synthesizer: ScoreSynthesizer? = null
    private var loading: Job? = null
    private var running: Job? = null
    private var previewing: Job? = null
    private var event = 0

    init {
        viewModelScope.launch {
            performed.collectLatest { shown ->
                val ref = shown?.part
                stop()
                release()
                _cursor.value = null
                val preferences = ref?.let(loadPreferences) ?: PlaybackPreferences()
                _state.update { PlaybackState(tempo = preferences.tempo, instrument = preferences.instrument, microphoneAvailable = it.microphoneAvailable) }
            }
        }
    }

    fun toggle() {
        if (_state.value.isOpen) close() else open()
    }

    fun close() {
        logger.info("Closing playback")
        stop()
        _cursor.value = null
        _state.update { it.copy(isOpen = false, error = null) }
    }

    fun selectMode(mode: PlaybackMode) {
        if (_state.value.mode == mode) return
        logger.info("Selecting playback mode {}", mode)
        stop()
        _state.update { it.copy(mode = mode, error = null) }
    }

    fun start() {
        val current = performance?.takeIf { it.events.isNotEmpty() } ?: return
        stop()
        _state.value.let { logger.info("Starting {} at event {} with tempo {} and instrument {}", it.mode, event, it.tempo, it.instrument) }
        _state.update { it.copy(isRunning = true, error = null) }
        running = viewModelScope.launch {
            when (_state.value.mode) {
                PlaybackMode.Listen -> listen(current)
                PlaybackMode.PlayAlong -> playAlong(current)
            }
            running = null
            _state.update { it.copy(isRunning = false) }
            showStill()
        }
    }

    fun stop() {
        previewing?.cancel()
        running?.let { logger.info("Stopping playback at event {}", event) }
        running?.cancel()
        running = null
        synthesizer?.let { synth -> viewModelScope.launch { synth.pause() } }
        _state.update { it.copy(isRunning = false) }
        showStill()
    }

    fun setTempo(tempo: Float) {
        val clamped = tempo.coerceIn(MIN_TEMPO, MAX_TEMPO)
        _state.update { it.copy(tempo = clamped) }
        synthesizer?.setTempo(clamped)
    }

    fun commitTempo() {
        logger.info("Changing tempo to {}", _state.value.tempo)
        savePreferences()
    }

    fun setInstrument(instrument: Instrument?) {
        if (_state.value.instrument == instrument) return
        logger.info("Changing instrument to {}", instrument)
        stop()
        synthesizer?.close()
        synthesizer = null
        _state.update { it.copy(instrument = instrument) }
        savePreferences()
    }

    private fun savePreferences() {
        val ref = performed.value?.part ?: return
        val current = _state.value
        storePreferences(ref, PlaybackPreferences(current.tempo, current.instrument))
    }

    fun select(note: String) {
        val current = performance ?: return
        val start = current.startOf(note) ?: return
        moveTo(current, current.eventAt(start), note, reveal = false)
    }

    fun step(offset: Int) {
        val current = performance?.takeIf { _state.value.isOpen && it.events.isNotEmpty() } ?: return
        val target = (event + offset).coerceIn(0, current.events.lastIndex)
        moveTo(current, target, current.events[target].notes.first().id, reveal = true)
    }

    private fun moveTo(current: ScorePerformance, index: Int, note: String, reveal: Boolean) {
        val wasRunning = running != null
        stop()
        event = index
        showStill(reveal)
        when {
            wasRunning -> start()
            _state.value.mode == PlaybackMode.Listen -> preview(current, current.events[event], note)
        }
    }

    private fun open() {
        logger.info("Opening playback for {}", performed.value?.part)
        _state.update { it.copy(isOpen = true, error = null) }
        if (_state.value.microphoneAvailable == null) {
            viewModelScope.launch { _state.update { it.copy(microphoneAvailable = isMicrophoneAvailable()) } }
        }
        if (performance != null) return showStill()
        if (loading?.isActive == true) return
        val ref = performed.value?.part ?: return
        loading = viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val loaded = loadPerformance(ref)
            logger.info("Loaded performance of {} with {} events", ref, loaded?.events?.size)
            performance = loaded
            event = 0
            _state.update { it.copy(isLoading = false, error = if (loaded == null) PlaybackError.Score else null) }
            showStill()
        }
    }

    private suspend fun listen(current: ScorePerformance) {
        val synth = synthesizer ?: openSynthesizer(current) ?: return
        synth.setTempo(_state.value.tempo)
        if (event >= current.events.lastIndex) event = 0
        synth.play(current.events[event].quarter)
        coroutineScope {
            val tracking = launch {
                synth.position.collect { position ->
                    event = current.eventAt(position).coerceAtLeast(0)
                    _cursor.value = PlaybackCursor(current.events[event].ids(), current.soundingAt(position).map { it.id }, follow = true)
                }
            }
            synth.isPlaying.first { !it }
            tracking.cancel()
        }
        event = 0
    }

    private suspend fun openSynthesizer(current: ScorePerformance): ScoreSynthesizer? {
        _state.update { it.copy(isLoading = true) }
        val opened = attempt { openScoreSynthesizer(current, _state.value.instrument) }
        synthesizer = opened
        _state.update { it.copy(isLoading = false, error = if (opened == null) PlaybackError.Synthesizer else null) }
        return opened
    }

    private fun preview(current: ScorePerformance, shown: PerformanceEvent, note: String) {
        val ref = performed.value?.part ?: return
        previewing = viewModelScope.launch {
            val written = if (shown.needsWrittenPitches) loadWrittenPitches(ref, shown.ids()) else emptyMap()
            val pitch = shown.pitchOf(note, written) ?: return@launch
            val synth = synthesizer ?: openSynthesizer(current) ?: return@launch
            synth.sound(pitch)
        }
    }

    private suspend fun playAlong(current: ScorePerformance) {
        val follower = ScoreFollower(current.events, event)
        if (follower.isFinished) return
        val listener = NoteListener(MICROPHONE_SAMPLE_RATE)
        event = follower.expected
        _cursor.value = PlaybackCursor(current.events[event].ids(), emptyList(), follow = true)
        microphoneSamples(MICROPHONE_SAMPLE_RATE)
            .map(listener::listen)
            .flowOn(Dispatchers.Default)
            .catch { e ->
                logger.warn("Listening to the microphone failed", e)
                _state.update { it.copy(error = PlaybackError.Microphone) }
            }
            .takeWhile { !follower.isFinished }
            .collect { pitches ->
                val hit = pitches.mapNotNull(follower::hear).lastOrNull() ?: return@collect
                event = follower.expected.coerceAtMost(current.events.lastIndex)
                _cursor.value = PlaybackCursor(current.events[event].ids(), current.events[hit].ids(), follow = true)
            }
        if (follower.isFinished) event = 0
    }

    private fun showStill(reveal: Boolean = false) {
        val current = performance?.takeIf { _state.value.isOpen } ?: return
        val shown = current.events.getOrNull(event) ?: return
        _cursor.value = PlaybackCursor(shown.ids(), emptyList(), follow = reveal)
    }

    private fun release() {
        loading?.cancel()
        synthesizer?.close()
        synthesizer = null
        performance = null
        event = 0
    }

    private fun PerformanceEvent.ids() = notes.map { it.id }

    override fun onCleared() = release()
}
