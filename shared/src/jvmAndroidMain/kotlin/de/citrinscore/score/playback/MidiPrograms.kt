package de.citrinscore.score.playback

import dev.atsushieno.ktmidi.Midi1Event
import dev.atsushieno.ktmidi.Midi1Message
import dev.atsushieno.ktmidi.Midi1Music
import dev.atsushieno.ktmidi.Midi1SimpleMessage
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.read
import dev.atsushieno.ktmidi.write

private const val PERCUSSION_CHANNEL = 9
private const val DEFAULT_PROGRAM = 0
private val ChannelStatuses = MidiChannelStatus.NOTE_OFF..MidiChannelStatus.PITCH_BEND

internal fun ByteArray.initialProgram(): Int =
    music().tracks.flatMap { it.events }.map { it.message }.firstOrNull { it.isProgramChange && it.isMelodic }?.msb?.toInt() ?: DEFAULT_PROGRAM

/** Lets the first notated instrument play [program] while every other instrument keeps its notated program. */
internal fun ByteArray.withLeadProgram(program: Int): ByteArray {
    val music = music()
    val lead = music.tracks.firstNotNullOfOrNull { track ->
        track.events.firstOrNull { it.message.isChannelMessage && it.message.isMelodic }?.message?.channel?.toInt()
    } ?: return this
    music.tracks.forEach { track ->
        if (track.events.none { it.message.isChannelMessage && it.message.channel.toInt() == lead }) return@forEach
        val replaced = track.events.map { event ->
            if (event.message.isProgramChange && event.message.channel.toInt() == lead) Midi1Event(event.deltaTime, programChange(lead, program)) else event
        }
        track.events.clear()
        track.events += listOf(Midi1Event(0, programChange(lead, program))) + replaced
    }
    return mutableListOf<Byte>().also(music::write).toByteArray()
}

private fun ByteArray.music() = Midi1Music().apply { read(toList()) }

private fun programChange(channel: Int, program: Int) = Midi1SimpleMessage(MidiChannelStatus.PROGRAM or channel, program, 0)

private val Midi1Message.status get() = statusCode.toInt() and 0xFF

private val Midi1Message.isChannelMessage get() = status in ChannelStatuses

private val Midi1Message.isProgramChange get() = status == MidiChannelStatus.PROGRAM

private val Midi1Message.isMelodic get() = channel.toInt() != PERCUSSION_CHANNEL
