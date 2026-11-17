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

            assertTrue(wide.all { it.startsWith("<svg") && "class=\"note\"" in it })
            assertTrue(narrow.size > wide.size)
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
    fun `invalid scores are rejected`() = runTest {
        assertFailsWith<IllegalStateException> { openMusicXmlEngraver("no music".encodeToByteArray(), compressed = false) }
    }

    companion object {
        fun score(measures: Int) = buildString {
            append("""<?xml version="1.0" encoding="UTF-8"?><score-partwise version="4.0">""")
            append("""<part-list><score-part id="P1"><part-name>Violine</part-name></score-part></part-list><part id="P1">""")
            repeat(measures) { index ->
                append("""<measure number="${index + 1}">""")
                if (index == 0) append("<attributes><divisions>1</divisions><key><fifths>0</fifths></key><time><beats>4</beats><beat-type>4</beat-type></time><clef><sign>G</sign><line>2</line></clef></attributes>")
                listOf("C", "D", "E", "F").forEach { step -> append("<note><pitch><step>$step</step><octave>5</octave></pitch><duration>1</duration><type>quarter</type></note>") }
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
