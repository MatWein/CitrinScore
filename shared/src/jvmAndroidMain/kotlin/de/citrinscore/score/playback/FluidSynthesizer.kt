package de.citrinscore.score.playback

import de.citrinscore.score.musicxml.ScorePerformance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.roundToInt

private const val BLOCK_FRAMES = 512
private const val PREVIEW_HOLD_FRAMES = ENGINE_SAMPLE_RATE * 3 / 4
private const val PREVIEW_RELEASE_FRAMES = ENGINE_SAMPLE_RATE / 2

actual suspend fun openScoreSynthesizer(performance: ScorePerformance, instrument: Instrument?): ScoreSynthesizer =
    FluidSynthesizer(FluidSynthEngine.open(performance, instrument), performance)

private class FluidSynthesizer(private val engine: FluidSynthEngine, private val performance: ScorePerformance) : ScoreSynthesizer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val control = Mutex()
    private var rendering: Job? = null

    private val _position = MutableStateFlow(0.0)
    override val position: StateFlow<Double> = _position.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    override suspend fun play(from: Double) = control.withLock {
        stopRendering()
        _position.value = from
        engine.start((from * performance.ticksPerQuarter).roundToInt())
        _isPlaying.value = true
        rendering = scope.launch { renderPlayback() }
    }

    override suspend fun pause() = control.withLock { stopRendering() }

    override suspend fun sound(pitch: Int) = control.withLock {
        stopRendering()
        engine.noteOn(pitch)
        rendering = scope.launch { renderPreview(pitch) }
    }

    override fun setTempo(factor: Float) = engine.setTempo(factor.toDouble())

    override fun close() {
        scope.launch {
            control.withLock {
                stopRendering()
                engine.close()
            }
            scope.cancel()
        }
    }

    private suspend fun stopRendering() {
        rendering?.let { job ->
            job.cancelAndJoin()
            engine.stop()
        }
        rendering = null
        _isPlaying.value = false
    }

    private fun CoroutineScope.renderPlayback() {
        val marks = ArrayDeque<Pair<Long, Int>>()
        render { written, played ->
            marks.addLast(written to engine.currentTick)
            while (marks.size > 1 && marks.first().first < played) marks.removeFirst()
            _position.value = minOf(marks.first().second.toDouble() / performance.ticksPerQuarter, performance.end)
            engine.isPlaying
        }
        if (isActive) {
            _position.value = performance.end
            _isPlaying.value = false
        }
    }

    private fun CoroutineScope.renderPreview(pitch: Int) {
        var held = true
        render { _, played ->
            if (held && played >= PREVIEW_HOLD_FRAMES) {
                engine.noteOff(pitch)
                held = false
            }
            played < PREVIEW_HOLD_FRAMES + PREVIEW_RELEASE_FRAMES
        }
    }

    private fun CoroutineScope.render(proceed: (written: Long, played: Long) -> Boolean) {
        val samples = ShortArray(BLOCK_FRAMES * ENGINE_CHANNELS)
        var written = 0L
        openPcmOutput(ENGINE_SAMPLE_RATE, ENGINE_CHANNELS).use { output ->
            do {
                engine.render(samples)
                output.write(samples)
                written += BLOCK_FRAMES
            } while (isActive && proceed(written, output.playedFrames))
        }
    }
}
