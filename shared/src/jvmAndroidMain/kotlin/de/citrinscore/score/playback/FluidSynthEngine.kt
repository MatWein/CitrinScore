package de.citrinscore.score.playback

import com.sun.jna.Memory
import com.sun.jna.Pointer
import de.citrinscore.score.musicxml.ScorePerformance
import de.citrinscore.util.extractResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.slf4j.LoggerFactory
import java.io.File

internal const val ENGINE_SAMPLE_RATE = 44100
internal const val ENGINE_CHANNELS = 2
private const val BYTES_PER_SAMPLE = 2
private const val GAIN = 0.5
private const val SOUND_FONT_RESOURCE = "files/GeneralUser-GS.sf2"
private const val SOUND_FONT_DIRECTORY = "soundfont"
private const val SOUND_FONT_FILE = "GeneralUser-GS.sf2"
private const val PREVIEW_CHANNEL = 15
private const val PREVIEW_VELOCITY = 96

private val soundFontExtraction = Mutex()
private var soundFont: File? = null
private val logger = LoggerFactory.getLogger("de.citrinscore.score.playback.FluidSynth")

internal class FluidSynthEngine private constructor(
    private val library: FluidSynthLibrary,
    private val settings: Pointer,
    private val synth: Pointer,
    private val player: Pointer,
    private val previewProgram: Int,
) : AutoCloseable {
    private var buffer = Memory(1)
    private var started = false

    val isPlaying get() = library.fluidPlayerGetStatus(player) == FluidSynthLibrary.PLAYER_PLAYING

    val currentTick get() = library.fluidPlayerGetCurrentTick(player)

    fun start(tick: Int) {
        library.fluidSynthAllSoundsOff(synth, FluidSynthLibrary.ALL_CHANNELS)
        library.fluidPlayerSeek(player, tick)
        library.fluidPlayerPlay(player)
        started = true
    }

    fun stop() {
        if (started) library.fluidPlayerStop(player)
    }

    fun noteOn(pitch: Int) {
        library.fluidSynthAllSoundsOff(synth, FluidSynthLibrary.ALL_CHANNELS)
        library.fluidSynthProgramChange(synth, PREVIEW_CHANNEL, previewProgram)
        library.fluidSynthNoteon(synth, PREVIEW_CHANNEL, pitch, PREVIEW_VELOCITY)
    }

    fun noteOff(pitch: Int) {
        library.fluidSynthNoteoff(synth, PREVIEW_CHANNEL, pitch)
    }

    fun setTempo(factor: Double) {
        library.fluidPlayerSetTempo(player, FluidSynthLibrary.TEMPO_INTERNAL, factor)
    }

    fun render(samples: ShortArray) {
        val bytes = samples.size.toLong() * BYTES_PER_SAMPLE
        if (buffer.size() != bytes) buffer = Memory(bytes)
        library.fluidSynthWriteS16(synth, samples.size / ENGINE_CHANNELS, buffer, 0, ENGINE_CHANNELS, buffer, 1, ENGINE_CHANNELS)
        buffer.read(0, samples, 0, samples.size)
    }

    override fun close() {
        library.deleteFluidPlayer(player)
        library.deleteFluidSynth(synth)
        library.deleteFluidSettings(settings)
    }

    companion object {
        suspend fun open(performance: ScorePerformance, instrument: Instrument?): FluidSynthEngine {
            val midi = instrument?.let { performance.midi.withLeadProgram(it.program) } ?: performance.midi
            return open(midi, instrument?.program)
        }

        suspend fun open(midi: ByteArray, previewProgram: Int? = null): FluidSynthEngine {
            val font = soundFontFile()
            return withContext(Dispatchers.IO) {
                logger.debug("Opening FluidSynth with {} MIDI bytes and sound font {}", midi.size, font)
                val previewProgram = previewProgram ?: midi.initialProgram()
                val library = FluidSynthLibrary.instance
                val settings = library.newFluidSettings()
                library.fluidSettingsSetnum(settings, "synth.sample-rate", ENGINE_SAMPLE_RATE.toDouble())
                library.fluidSettingsSetnum(settings, "synth.gain", GAIN)
                library.fluidSettingsSetstr(settings, "player.timing-source", "sample")
                val synth = library.newFluidSynth(settings)
                val player = synth?.let(library::newFluidPlayer)
                val loaded = synth != null && player != null && library.fluidSynthSfload(synth, font.absolutePath, 1) >= 0 &&
                    library.fluidPlayerAddMem(player, midi, SizeT(midi.size.toLong())) == FluidSynthLibrary.OK
                if (!loaded) {
                    player?.let(library::deleteFluidPlayer)
                    synth?.let(library::deleteFluidSynth)
                    library.deleteFluidSettings(settings)
                    error("FluidSynth could not be initialised, synthesizer: ${synth != null}, player: ${player != null}")
                }
                logger.debug("Opened FluidSynth")
                FluidSynthEngine(library, settings, synth, player, previewProgram)
            }
        }

        private suspend fun soundFontFile(): File = soundFontExtraction.withLock {
            soundFont?.takeIf { it.exists() } ?: File(
                extractResource(SOUND_FONT_RESOURCE, SOUND_FONT_DIRECTORY) { content, target -> File(target, SOUND_FONT_FILE).writeBytes(content) },
                SOUND_FONT_FILE,
            ).also { soundFont = it }
        }
    }
}
