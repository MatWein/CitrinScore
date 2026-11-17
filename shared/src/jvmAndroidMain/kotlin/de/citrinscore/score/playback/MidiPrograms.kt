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

internal fun ByteArray.withProgram(program: Int): ByteArray {
    val music = music()
    music.tracks.forEach { track ->
        val channels = track.events.map { it.message }.filter { it.isChannelMessage && it.isMelodic }.map { it.channel.toInt() }.distinct()
        val replaced = track.events.map { event ->
            if (event.message.isProgramChange && event.message.isMelodic) Midi1Event(event.deltaTime, programChange(event.message.channel.toInt(), program)) else event
        }
        track.events.clear()
        track.events += channels.map { Midi1Event(0, programChange(it, program)) } + replaced
    }
    return mutableListOf<Byte>().also(music::write).toByteArray()
}

private fun ByteArray.music() = Midi1Music().apply { read(toList()) }

private fun programChange(channel: Int, program: Int) = Midi1SimpleMessage(MidiChannelStatus.PROGRAM or channel, program, 0)

private val Midi1Message.status get() = statusCode.toInt() and 0xFF

private val Midi1Message.isChannelMessage get() = status in ChannelStatuses

private val Midi1Message.isProgramChange get() = status == MidiChannelStatus.PROGRAM

private val Midi1Message.isMelodic get() = channel.toInt() != PERCUSSION_CHANNEL
