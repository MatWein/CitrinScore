package de.citrinscore.score.playback

import com.sun.jna.FunctionMapper
import com.sun.jna.IntegerType
import com.sun.jna.Library
import com.sun.jna.Native
import com.sun.jna.Pointer
import de.citrinscore.util.loadNativeLibrary

private const val LIBRARY_NAME = "fluidsynth"
private val WordStart = Regex("([a-z0-9])([A-Z])")

internal interface FluidSynthLibrary : Library {
    fun newFluidSettings(): Pointer

    fun deleteFluidSettings(settings: Pointer)

    fun fluidSettingsSetstr(settings: Pointer, name: String, value: String): Int

    fun fluidSettingsSetnum(settings: Pointer, name: String, value: Double): Int

    fun newFluidSynth(settings: Pointer): Pointer?

    fun deleteFluidSynth(synth: Pointer)

    fun fluidSynthSfload(synth: Pointer, filename: String, resetPresets: Int): Int

    fun fluidSynthAllSoundsOff(synth: Pointer, channel: Int): Int

    fun fluidSynthNoteon(synth: Pointer, channel: Int, key: Int, velocity: Int): Int

    fun fluidSynthNoteoff(synth: Pointer, channel: Int, key: Int): Int

    fun fluidSynthProgramChange(synth: Pointer, channel: Int, program: Int): Int

    fun fluidSynthWriteS16(
        synth: Pointer,
        frames: Int,
        left: Pointer,
        leftOffset: Int,
        leftIncrement: Int,
        right: Pointer,
        rightOffset: Int,
        rightIncrement: Int,
    ): Int

    fun newFluidPlayer(synth: Pointer): Pointer?

    fun deleteFluidPlayer(player: Pointer)

    fun fluidPlayerAddMem(player: Pointer, buffer: ByteArray, length: SizeT): Int

    fun fluidPlayerPlay(player: Pointer): Int

    fun fluidPlayerStop(player: Pointer): Int

    fun fluidPlayerSeek(player: Pointer, ticks: Int): Int

    fun fluidPlayerGetStatus(player: Pointer): Int

    fun fluidPlayerGetCurrentTick(player: Pointer): Int

    fun fluidPlayerSetTempo(player: Pointer, tempoType: Int, tempo: Double): Int

    companion object {
        const val OK = 0
        const val PLAYER_PLAYING = 1
        const val TEMPO_INTERNAL = 0
        const val ALL_CHANNELS = -1

        val instance: FluidSynthLibrary by lazy {
            loadNativeLibrary(LIBRARY_NAME) {
                Native.load(
                    LIBRARY_NAME,
                    FluidSynthLibrary::class.java,
                    mapOf(
                        Library.OPTION_STRING_ENCODING to Charsets.UTF_8.name(),
                        Library.OPTION_FUNCTION_MAPPER to FunctionMapper { _, method -> method.name.replace(WordStart, "$1_$2").lowercase() },
                    ),
                )
            }
        }
    }
}

internal class SizeT(value: Long = 0) : IntegerType(Native.SIZE_T_SIZE, value, true) {
    override fun toByte() = toLong().toByte()

    override fun toShort() = toLong().toShort()
}
