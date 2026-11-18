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
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import org.xml.sax.InputSource
import org.xml.sax.SAXException
import java.io.StringReader
import java.io.StringWriter
import java.util.Base64
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import kotlin.math.roundToInt
import kotlin.time.measureTimedValue

private const val PAGE_MARGIN = 50
private const val CLASS_STYLES = ".placeholder {display:none} .ending, .fing, .reh, .tempo {font-weight:bold} .dir, .dynam, .mNum {font-style:italic} .label {font-weight:normal}"
private const val TIMEMAP_OPTIONS = """{"includeMeasures":false,"includeRests":false}"""
private const val UTF8_BOM = "﻿"
private const val TICK_TOLERANCE = 1
private const val MXL_CONTAINER = "META-INF/container.xml"
private const val MXL_ROOTFILE = "rootfile"
private const val MXL_ROOTFILE_PATH = "full-path"
private const val SCORE_PART = "score-part"
private const val PRINT_OBJECT = "print-object"
private const val NO = "no"
private val PART_LABELS = listOf("part-name", "part-abbreviation")
private val Utf16Boms = listOf(byteArrayOf(0xFE.toByte(), 0xFF.toByte()), byteArrayOf(0xFF.toByte(), 0xFE.toByte()))
private val TimemapJson = Json { ignoreUnknownKeys = true }
private val logger = LoggerFactory.getLogger("de.citrinscore.score.musicxml.Verovio")

actual suspend fun openMusicXmlEngraver(data: ByteArray, compressed: Boolean): MusicXmlEngraver {
    logger.debug("Loading MusicXML with {} bytes, compressed: {}", data.size, compressed)
    val score = withContext(Dispatchers.Default) { (if (compressed) data.uncompressMusicXml() else data).decodeXml().withoutSinglePartName() }
    val resources = verovioResourcePath()
    val (toolkit, duration) = measureTimedValue {
        Verovio.call {
            val toolkit = constructorResourcePath(resources)
            setOptions(toolkit, options(EngravingLayout.Print))
            val loaded = loadData(toolkit, score)
            val log = getLog(toolkit)
            if (!loaded) {
                destructor(toolkit)
                error("Verovio could not load the score: $log")
            }
            logVerovio(log)
            toolkit
        }
    }
    logger.debug("Verovio loaded the score in {}", duration)
    return VerovioEngraver(GuardedResource(toolkit, Verovio::release))
}

private class VerovioEngraver(private val toolkit: GuardedResource<Pointer>) : MusicXmlEngraver {
    override suspend fun engrave(layout: EngravingLayout): List<EngravedPage> {
        val (pages, duration) = measureTimedValue {
            toolkit.use { handle ->
                Verovio.call {
                    setOptions(handle, options(layout))
                    redoLayout(handle, "{}")
                    val pages = (1..getPageCount(handle)).map { page -> renderToSVG(handle, page, false) }
                    drainLog(handle)
                    pages
                }
            }
        }
        logger.debug("Verovio engraved {} pages for {} in {}", pages.size, layout, duration)
        val glyphs = verovioGlyphBounds()
        val engraved = withContext(Dispatchers.Default) {
            var firstMeasure = 0
            pages.map { svg ->
                val geometry = pageGeometry(svg, firstMeasure, glyphs)
                firstMeasure = (geometry.staves.maxOfOrNull { it.measure } ?: (firstMeasure - 1)) + 1
                EngravedPage(svg, geometry.staves, geometry.notes)
            }
        }
        check(engraved.any { it.staves.isNotEmpty() }) { "The score contains no staves" }
        return engraved
    }

    override suspend fun perform(): ScorePerformance {
        val (rendered, duration) = measureTimedValue {
            toolkit.use { handle ->
                Verovio.call {
                    val timemap = TimemapJson.decodeFromString<List<TimemapEntry>>(renderToTimemap(handle, TIMEMAP_OPTIONS))
                    val midi = Base64.getDecoder().decode(renderToMIDI(handle))
                    val ids = timemap.flatMap { it.on }.distinct().associateWith { getNotatedIdForElement(handle, it) }
                    drainLog(handle)
                    Triple(timemap, midi, ids)
                }
            }
        }
        val (timemap, midi, notated) = rendered
        logger.debug("Verovio rendered {} timemap entries and {} MIDI bytes in {}", timemap.size, midi.size, duration)
        return withContext(Dispatchers.Default) { performance(timemap, midi, notated) }
    }

    override suspend fun writtenPitches(notes: Collection<String>): Map<String, Int> = toolkit.use { handle ->
        Verovio.call {
            val values = notes.mapNotNull { note -> TimemapJson.decodeFromString<MidiValues>(getMIDIValuesForElement(handle, note)).pitch?.let { note to it } }
            drainLog(handle)
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

private fun VerovioLibrary.drainLog(toolkit: Pointer) = logVerovio(getLog(toolkit))

private fun logVerovio(log: String) {
    if (log.isNotBlank()) logger.debug("Verovio reported: {}", log.trim())
}

private fun ByteArray.uncompressMusicXml(): ByteArray {
    val entries = ZipInputStream(inputStream()).use { zip ->
        generateSequence { zip.nextEntry }.filterNot { it.isDirectory }.associate { it.name to zip.readBytes() }
    }
    val container = entries[MXL_CONTAINER] ?: error("The archive contains no $MXL_CONTAINER")
    val rootfile = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(container.inputStream())
        .getElementsByTagName(MXL_ROOTFILE).item(0) as? Element
    val path = rootfile?.getAttribute(MXL_ROOTFILE_PATH)?.takeIf { it.isNotEmpty() } ?: error("$MXL_CONTAINER names no score")
    return entries[path] ?: error("The archive contains no $path")
}

private fun String.withoutSinglePartName(): String {
    val builder = DocumentBuilderFactory.newInstance().newDocumentBuilder().apply { setEntityResolver { _, _ -> InputSource(StringReader("")) } }
    val document = try {
        builder.parse(InputSource(StringReader(this)))
    } catch (e: SAXException) {
        logger.debug("Leaving unparsable MusicXML to Verovio", e)
        return this
    }
    val parts = document.getElementsByTagName(SCORE_PART)
    if (parts.length != 1) return this
    val part = parts.item(0) as Element
    PART_LABELS.forEach { label ->
        val names = part.getElementsByTagName(label)
        (0 until names.length).forEach { (names.item(it) as Element).setAttribute(PRINT_OBJECT, NO) }
    }
    return StringWriter().also { TransformerFactory.newInstance().newTransformer().transform(DOMSource(document), StreamResult(it)) }.toString()
}

private fun ByteArray.decodeXml(): String =
    if (Utf16Boms.any { bom -> bom.indices.all { getOrNull(it) == bom[it] } }) toString(Charsets.UTF_16) else decodeToString().removePrefix(UTF8_BOM)
