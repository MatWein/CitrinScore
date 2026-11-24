package de.citrinscore.score.musicxml

import io.github.vinceglb.filekit.FileKit
import kotlinx.coroutines.test.runTest
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MusicXmlEngraverTest {

    private val filesDir = Files.createTempDirectory("citrinscore-files").toFile()

    @BeforeTest
    fun setUp() {
        FileKit.init(appId = "citrinscore-test", filesDir = filesDir)
    }

    @AfterTest
    fun cleanUp() {
        filesDir.deleteRecursively()
    }

    @Test
    fun `scores are engraved to one svg per page and reflow with the page width`() = runTest {
        openMusicXmlEngraver(score(measures = 40).encodeToByteArray(), compressed = false).use { engraver ->
            val wide = engraver.engrave(EngravingLayout(pageWidth = 2100, pageHeight = 2970))
            val narrow = engraver.engrave(EngravingLayout(pageWidth = 700, pageHeight = 1000))

            assertTrue(wide.all { it.svg.startsWith("<svg") && "class=\"note\"" in it.svg })
            assertTrue(narrow.size > wide.size)
            listOf(wide, narrow).forEach { pages ->
                assertEquals((0 until 40).toList(), pages.flatMap { page -> page.staves.map { it.measure } })
                pages.forEach { page ->
                    assertTrue(page.staves.all { it.staff == 0 && it.bounds.width > 0f && it.space > 0f && it.bounds.height == it.space * 4 })
                }
            }
        }
    }

    @Test
    fun `compressed scores and byte order marks are supported`() = runTest {
        val utf16 = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + score(measures = 1).toByteArray(Charsets.UTF_16LE)
        val compressed = mxl(score(measures = 1))

        openMusicXmlEngraver(utf16, compressed = false).use { assertEquals(1, it.engrave(EngravingLayout.Print).size) }
        openMusicXmlEngraver(compressed, compressed = true).use { assertEquals(1, it.engrave(EngravingLayout.Print).size) }
    }

    @Test
    fun `performances list every engraved note with its pitch`() = runTest {
        openMusicXmlEngraver(score(measures = 2).encodeToByteArray(), compressed = false).use { engraver ->
            val notes = engraver.engrave(EngravingLayout.Print).flatMap { it.notes }
            val performance = engraver.perform()

            assertEquals((0 until 8).map { it.toDouble() }, performance.events.map { it.quarter })
            assertEquals(listOf(72, 74, 76, 77, 72, 74, 76, 77), performance.events.map { it.pitches.single() })
            assertEquals(notes.map { it.id }.sorted(), performance.events.flatMap { event -> event.notes.map { it.id } }.sorted())
            assertTrue(notes.all { it.bounds.width > 0f && it.bounds.height > 0f })
            assertEquals(8.0, performance.end)
        }
    }

    @Test
    fun `tied notes are not played again`() = runTest {
        val tied = score(
            measures = 1,
            notes = listOf(note("C", tie = "start"), note("C", tie = "stop"), note("E"), note("F")).joinToString(""),
        )
        openMusicXmlEngraver(tied.encodeToByteArray(), compressed = false).use { engraver ->
            assertEquals(listOf(setOf(72), emptySet(), setOf(76), setOf(77)), engraver.perform().events.map { it.pitches })
        }
    }

    @Test
    fun `chord notes of transposing instruments sound at their transposed pitch`() = runTest {
        val chord = note("C") + note("E").replace("<note>", "<note><chord/>") + listOf("D", "E", "F").joinToString("") { note(it) }
        val clarinet = score(measures = 1, notes = chord, attributes = "<transpose><diatonic>-1</diatonic><chromatic>-2</chromatic></transpose>")
        openMusicXmlEngraver(clarinet.encodeToByteArray(), compressed = false).use { engraver ->
            val first = engraver.perform().events.first()
            val written = engraver.writtenPitches(first.notes.map { it.id })

            assertEquals(setOf(70, 74), first.pitches)
            assertEquals(listOf(72, 76), first.notes.map { written.getValue(it.id) })
            assertEquals(listOf(70, 74), first.notes.map { first.pitchOf(it.id, written) })
        }
    }

    @Test
    fun `the part name is only printed when the score has several parts`() = runTest {
        val single = score(measures = 1)
        val duet = single.replace("</score-part>", "</score-part><score-part id=\"P2\"><part-name>Klavier</part-name></score-part>")
            .replace("</score-partwise>", single.substringAfter("</part-list>").substringBefore("</score-partwise>").replace("\"P1\"", "\"P2\"") + "</score-partwise>")

        openMusicXmlEngraver(single.encodeToByteArray(), compressed = false).use { engraver ->
            assertFalse(engraver.engrave(EngravingLayout.Print).any { "Violine" in it.svg })
        }
        openMusicXmlEngraver(duet.encodeToByteArray(), compressed = false).use { engraver ->
            val svg = engraver.engrave(EngravingLayout.Print).joinToString { it.svg }
            assertTrue("Violine" in svg && "Klavier" in svg)
        }
    }

    @Test
    fun `repeat counts above two are written above the repeat`() = runTest {
        fun withRepeat(times: String, words: String = "") = score(measures = 2).replaceFirst(
            "</measure>",
            words + """<barline location="right"><bar-style>light-heavy</bar-style><repeat direction="backward"$times/></barline></measure>""",
        )

        suspend fun texts(score: String) = openMusicXmlEngraver(score.encodeToByteArray(), compressed = false).use { engraver ->
            Regex(">([^<>]*\\dx)<").findAll(engraver.engrave(EngravingLayout.Print).joinToString { it.svg }).map { it.groupValues[1] }.toList()
        }

        assertEquals(listOf("3x"), texts(withRepeat(" times=\"3\"")))
        assertEquals(emptyList(), texts(withRepeat("")))
        assertEquals(emptyList(), texts(withRepeat(" times=\"2\"")))
        val labeled = """<direction placement="above"><direction-type><words>3 mal</words></direction-type></direction>"""
        assertEquals(emptyList(), texts(withRepeat(" times=\"3\"", labeled)))
    }

    @Test
    fun `multi measure rests are drawn as bars however short they are`() = runTest {
        val score = score(measures = 1, notes = """<note><rest measure="yes"/><duration>4</duration></note>""", attributes = "<measure-style><multiple-rest>3</multiple-rest></measure-style>")
            .replace("</part>", (2..3).joinToString("") { """<measure number="$it"><note><rest measure="yes"/><duration>4</duration></note></measure>""" } + "</part>")

        openMusicXmlEngraver(score.encodeToByteArray(), compressed = false).use { engraver ->
            val svg = engraver.engrave(EngravingLayout.Print).joinToString { it.svg }
            assertTrue("class=\"multiRest\"" in svg)
            assertFalse(listOf("E4E1", "E4E2", "E4E3").any { it in svg })
        }
    }

    @Test
    fun `invalid scores are rejected`() = runTest {
        assertFailsWith<IllegalStateException> { openMusicXmlEngraver("no music".encodeToByteArray(), compressed = false) }
        openMusicXmlEngraver("<score-partwise>".encodeToByteArray(), compressed = false).use { engraver ->
            assertFailsWith<IllegalStateException> { engraver.engrave(EngravingLayout.Print) }
        }
        assertFailsWith<IllegalStateException> { openMusicXmlEngraver("not a score".encodeToByteArray(), compressed = true) }
    }

    companion object {
        fun note(step: String, tie: String? = null) = buildString {
            append("<note><pitch><step>$step</step><octave>5</octave></pitch><duration>1</duration>")
            tie?.let { append("""<tie type="$it"/>""") }
            append("<type>quarter</type>")
            tie?.let { append("""<notations><tied type="$it"/></notations>""") }
            append("</note>")
        }

        fun score(measures: Int, notes: String = listOf("C", "D", "E", "F").joinToString("") { note(it) }, attributes: String = "") = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?><score-partwise version="4.0">""")
            append("""<part-list><score-part id="P1"><part-name>Violine</part-name></score-part></part-list><part id="P1">""")
            repeat(measures) { index ->
                append("""<measure number="${index + 1}">""")
                if (index == 0) append("<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef>$attributes</attributes>")
                append(notes)
                append("</measure>")
            }
            append("</part></score-partwise>")
        }

        fun mxl(score: String): ByteArray {
            val bytes = ByteArrayOutputStream()
            ZipOutputStream(bytes).use { zip ->
                zip.putNextEntry(ZipEntry("META-INF/container.xml"))
                zip.write("""<?xml version="1.0" encoding="UTF-8"?><container><rootfiles><rootfile full-path="score.musicxml"/></rootfiles></container>""".encodeToByteArray())
                zip.putNextEntry(ZipEntry("score.musicxml"))
                zip.write(score.encodeToByteArray())
            }
            return bytes.toByteArray()
        }
    }
}
