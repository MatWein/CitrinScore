package de.citrinscore.score.playback

import de.citrinscore.score.audio.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlin.time.Duration

private const val BLOCK_FRAMES = 512

/** Plays a MIDI file attachment with the sound font of the score playback. */
internal suspend fun openMidiAudioPlayer(midi: ByteArray): AudioPlayer {
    val timeline = MidiTimeline(midi)
    return MidiAudioPlayer(FluidSynthEngine.open(midi), timeline)
}

// All engine calls run one after another on a single thread: the rendering loop yields after every block, so play,
// pause and seek are applied between two blocks
@OptIn(ExperimentalCoroutinesApi::class)
private class MidiAudioPlayer(private val engine: FluidSynthEngine, private val timeline: MidiTimeline) : AudioPlayer {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private var rendering: Job? = null

    @Volatile
    private var tick = 0

    @Volatile
    override var isPlaying = false
        private set

    override val duration: Duration = timeline.duration

    override val position get() = timeline.timeAt(tick)

    override fun play() {
        if (isPlaying) return
        if (tick >= timeline.totalTicks) tick = 0
        isPlaying = true
        val start = tick
        scope.launch { restart(start) }
    }

    override fun pause() {
        isPlaying = false
        scope.launch { stopRendering() }
    }

    override fun seekTo(position: Duration) {
        val target = timeline.tickAt(position)
        tick = target
        if (isPlaying) scope.launch { restart(target) }
    }

    override fun close() {
        isPlaying = false
        scope.launch {
            stopRendering()
            engine.close()
            scope.cancel()
        }
    }

    private suspend fun restart(start: Int) {
        stopRendering()
        if (!isPlaying) return
        engine.start(start)
        rendering = scope.launch { render() }
    }

    private suspend fun stopRendering() {
        rendering?.let { job ->
            job.cancelAndJoin()
            engine.stop()
        }
        rendering = null
    }

    private suspend fun render() {
        val samples = ShortArray(BLOCK_FRAMES * ENGINE_CHANNELS)
        val marks = ArrayDeque<Pair<Long, Int>>()
        var written = 0L
        openPcmOutput(ENGINE_SAMPLE_RATE, ENGINE_CHANNELS).use { output ->
            do {
                engine.render(samples)
                output.write(samples)
                written += BLOCK_FRAMES
                marks.addLast(written to engine.currentTick)
                while (marks.size > 1 && marks.first().first < output.playedFrames) marks.removeFirst()
                tick = marks.first().second.coerceIn(0, timeline.totalTicks)
                yield()
            } while (engine.isPlaying)
        }
        tick = timeline.totalTicks
        isPlaying = false
    }
}
