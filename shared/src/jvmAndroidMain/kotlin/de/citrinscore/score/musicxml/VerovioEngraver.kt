package de.citrinscore.score.musicxml

import com.sun.jna.Pointer
import de.citrinscore.util.GuardedResource
import dev.atsushieno.ktmidi.Midi1Music
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.read
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64
import kotlin.math.roundToInt

private const val PAGE_MARGIN = 50
private const val CLASS_STYLES = ".placeholder {display:none} .ending, .fing, .reh, .tempo {font-weight:bold} .dir, .dynam, .mNum {font-style:italic} .label {font-weight:normal}"
private const val TIMEMAP_OPTIONS = """{"includeMeasures":false,"includeRests":false}"""
private const val UTF8_BOM = "﻿"
private const val TICK_TOLERANCE = 1
private val Utf16Boms = listOf(byteArrayOf(0xFE.toByte(), 0xFF.toByte()), byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
private val TimemapJson = Json { ignoreUnknownKeys = true }

actual suspend fun openMusicXmlEngraver(data: ByteArray, compressed: Boolean): MusicXmlEngraver {
    val resources = verovioResourcePath()
    val toolkit = Verovio.call {
        val toolkit = constructorResourcePath(resources)
        setOptions(toolkit, options(EngravingLayout.Print))
        val loaded = if (compressed) loadZipDataBuffer(toolkit, data, data.size) else loadData(toolkit, data.decodeXml())
        val log = getLog(toolkit)
        if (!loaded) {
            destructor(toolkit)
            error("Verovio could not load the score: $log")
        }
        toolkit
    }
    return VerovioEngraver(GuardedResource(toolkit, Verovio::release))
}

private class VerovioEngraver(private val toolkit: GuardedResource<Pointer>) : MusicXmlEngraver {
    override suspend fun engrave(layout: EngravingLayout): List<EngravedPage> {
        val pages = toolkit.use { handle ->
            Verovio.call {
                setOptions(handle, options(layout))
                redoLayout(handle, "{}")
                val pages = (1..getPageCount(handle)).map { page -> renderToSVG(handle, page, false) }
                getLog(handle)
                pages
            }
        }
        val glyphs = verovioGlyphBounds()
        return withContext(Dispatchers.Default) {
            var firstMeasure = 0
            pages.map { svg ->
                val geometry = pageGeometry(svg, firstMeasure, glyphs)
                firstMeasure = (geometry.staves.maxOfOrNull { it.measure } ?: (firstMeasure - 1)) + 1
                EngravedPage(svg, geometry.staves, geometry.notes)
            }
        }
    }

    override suspend fun perform(): ScorePerformance {
        val (timemap, midi, notated) = toolkit.use { handle ->
            Verovio.call {
                val timemap = TimemapJson.decodeFromString<List<TimemapEntry>>(renderToTimemap(handle, TIMEMAP_OPTIONS))
                val midi = Base64.getDecoder().decode(renderToMIDI(handle))
                val ids = timemap.flatMap { it.on }.distinct().associateWith { getNotatedIdForElement(handle, it) }
                getLog(handle)
                Triple(timemap, midi, ids)
            }
        }
        return withContext(Dispatchers.Default) { performance(timemap, midi, notated) }
    }

    override suspend fun writtenPitches(notes: Collection<String>): Map<String, Int> = toolkit.use { handle ->
        Verovio.call {
            val values = notes.mapNotNull { note -> TimemapJson.decodeFromString<MidiValues>(getMIDIValuesForElement(handle, note)).pitch?.let { note to it } }
            getLog(handle)
            values.toMap()
        }
    }

    override fun close() = toolkit.close()
}

@Serializable
private class TimemapEntry(val qstamp: Double, val on: List<String> = emptyList(), val off: List<String> = emptyList())

@Serializable
private class MidiValues(val pitch: Int? = null)

private fun performance(timemap: List<TimemapEntry>, midi: ByteArray, notated: Map<String, String>): ScorePerformance {
    val music = Midi1Music().apply { read(midi.toList()) }
    val ticksPerQuarter = music.deltaTimeSpec
    val onsets = noteOnsets(music)
    val ends = timemap.flatMap { entry -> entry.off.map { it to entry.qstamp } }.toMap()
    val events = timemap.filter { it.on.isNotEmpty() }.map { entry ->
        val tick = (entry.qstamp * ticksPerQuarter).roundToInt()
        PerformanceEvent(
            quarter = entry.qstamp,
            notes = entry.on.map { id -> PerformedNote(notated.getValue(id), entry.qstamp, ends[id] ?: entry.qstamp) },
            pitches = (tick - TICK_TOLERANCE..tick + TICK_TOLERANCE).flatMap { onsets[it].orEmpty() }.toSet(),
        )
    }
    return ScorePerformance(midi, ticksPerQuarter, events)
}

private fun noteOnsets(music: Midi1Music): Map<Int, List<Int>> = music.tracks.flatMap { track ->
    var tick = 0
    track.events.mapNotNull { event ->
        tick += event.deltaTime
        val message = event.message
        (tick to message.msb.toInt()).takeIf { message.statusCode.toInt() and 0xFF == MidiChannelStatus.NOTE_ON && message.lsb > 0 }
    }
}.groupBy({ it.first }, { it.second })

private fun options(layout: EngravingLayout) = buildJsonObject {
    put("pageWidth", layout.pageWidth)
    put("pageHeight", layout.pageHeight)
    put("pageMarginLeft", PAGE_MARGIN)
    put("pageMarginRight", PAGE_MARGIN)
    put("pageMarginTop", PAGE_MARGIN)
    put("pageMarginBottom", PAGE_MARGIN)
    put("adjustPageHeight", true)
    put("breaks", "auto")
    put("footer", "none")
    put("svgCss", CLASS_STYLES)
    put("svgViewBox", true)
}.toString()

private fun ByteArray.decodeXml(): String =
    if (Utf16Boms.any { bom -> bom.indices.all { getOrNull(it) == bom[it] } }) toString(Charsets.UTF_16) else decodeToString().removePrefix(UTF8_BOM)
