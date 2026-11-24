package de.citrinscore.score.playback

import dev.atsushieno.ktmidi.Midi1CompoundMessage
import dev.atsushieno.ktmidi.Midi1Music
import dev.atsushieno.ktmidi.Midi1Status
import dev.atsushieno.ktmidi.MidiMetaType
import dev.atsushieno.ktmidi.read
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.DurationUnit

private const val DEFAULT_TEMPO = 500_000L

/** Converts between the ticks of a MIDI file and the time they are played at, following its tempo changes. */
internal class MidiTimeline(midi: ByteArray) {
    private class TempoChange(val tick: Long, val micros: Double, val microsPerQuarter: Long)

    private val ticksPerQuarter: Int
    private val changes: List<TempoChange>

    val totalTicks: Int

    init {
        val music = Midi1Music().apply { read(midi.toList()) }
        require(music.deltaTimeSpec > 0) { "MIDI files with SMPTE timing are not supported" }
        ticksPerQuarter = music.deltaTimeSpec
        val tempos = mutableListOf<Pair<Long, Long>>()
        var end = 0L
        for (track in music.tracks) {
            var tick = 0L
            for (event in track.events) {
                tick += event.deltaTime
                val message = event.message
                if ((message.statusCode.toInt() and 0xFF) == Midi1Status.META && message.msb.toInt() == MidiMetaType.TEMPO) {
                    val data = (message as Midi1CompoundMessage).extraData ?: continue
                    tempos += tick to Midi1Music.getSmfTempo(data, message.extraDataOffset).toLong()
                }
            }
            end = maxOf(end, tick)
        }
        totalTicks = end.toInt()
        changes = tempos.sortedBy { it.first }.fold(listOf(TempoChange(0, 0.0, DEFAULT_TEMPO))) { list, (tick, tempo) ->
            val last = list.last()
            list + TempoChange(tick, last.micros + (tick - last.tick).toDouble() * last.microsPerQuarter / ticksPerQuarter, tempo)
        }
    }

    val duration: Duration get() = timeAt(totalTicks)

    fun timeAt(tick: Int): Duration {
        val change = changes.last { it.tick <= tick }
        return (change.micros + (tick - change.tick).toDouble() * change.microsPerQuarter / ticksPerQuarter).microseconds
    }

    fun tickAt(time: Duration): Int {
        val micros = time.toDouble(DurationUnit.MICROSECONDS).coerceAtLeast(0.0)
        val change = changes.last { it.micros <= micros }
        val tick = change.tick + (micros - change.micros) * ticksPerQuarter / change.microsPerQuarter
        return tick.toInt().coerceIn(0, totalTicks)
    }
}
