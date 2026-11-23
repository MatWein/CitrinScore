package de.citrinscore.library

import de.citrinscore.score.AttachmentKind
import de.citrinscore.score.CitScore
import de.citrinscore.score.InkStroke
import de.citrinscore.score.PageCrop
import de.citrinscore.score.PartDisplay
import de.citrinscore.score.scan.EraserStroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.IntSize
import de.citrinscore.score.render.openPageSource
import de.citrinscore.score.scan.DEFAULT_SCAN_THRESHOLD
import de.citrinscore.score.scan.SCAN_PAGE_WIDTH
import io.github.vinceglb.filekit.ImageFormat
import io.github.vinceglb.filekit.dialogs.compose.util.encodeToByteArray
import io.github.vinceglb.filekit.readBytes
import kotlin.math.roundToInt
import de.citrinscore.score.PlaybackPreferences
import de.citrinscore.score.RecognitionLayout
import de.citrinscore.score.RecognizedMeasure
import de.citrinscore.score.RecognizedMusic
import de.citrinscore.score.RecognizedNote
import de.citrinscore.score.playback.Instrument
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreDeviceState
import de.citrinscore.score.ScrollMode
import de.citrinscore.score.shared
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScorePart
import de.citrinscore.score.ScoreViewState
import de.citrinscore.score.TextNote
import de.citrinscore.score.render.PdfSourceTest
import de.citrinscore.score.render.ScoreRenderers
import de.citrinscore.score.render.contentCrop
import de.citrinscore.score.render.openImageSource
import de.citrinscore.score.render.openPdfSource
import de.citrinscore.serialization.AppJson
import de.citrinscore.score.musicxml.MusicXmlEngraverTest
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.util.zip.ZipFile
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

class LibraryRepositoryTest {

    private val root = Files.createTempDirectory("citrinscore-library").toFile()
    private var now = Instant.parse("2026-01-01T00:00:00Z")
    private val repository = LibraryRepository(clock = object : Clock {
        override fun now() = this@LibraryRepositoryTest.now
    })

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `folders come first by name and scores by last activity`() = runTest {
        File(root, "Zeta").mkdir()
        File(root, "alpha").mkdir()
        File(root, ".hidden").mkdir()
        File(root, "notes.txt").writeText("")
        File(root, "Broken.citscore").mkdir()
        importAt("Old", Instant.parse("2026-01-01T00:00:00Z"))
        importAt("New", Instant.parse("2026-03-01T00:00:00Z"))
        score("Reopened", createdAt = "2025-01-01T00:00:00Z", lastOpenedAt = "2026-06-01T00:00:00Z")

        val content = repository.content(PlatformFile(root))

        assertEquals(listOf("alpha", "Zeta", "Reopened", "New", "Old", "Broken"), content.arranged(LibrarySort.RecentlyUsed, "").map { it.displayName })
        assertEquals(listOf("alpha", "Zeta", "Broken", "New", "Old", "Reopened"), content.arranged(LibrarySort.Title, "").map { it.displayName })
    }

    @Test
    fun `search matches names ignoring case`() = runTest {
        File(root, "Chorwerke").mkdir()
        importAt("Choral", Instant.parse("2026-01-01T00:00:00Z"))
        importAt("Etude", Instant.parse("2026-02-01T00:00:00Z"))

        val matches = repository.content(PlatformFile(root)).arranged(LibrarySort.Title, " cHOR ").map { it.displayName }

        assertEquals(listOf("Chorwerke", "Choral"), matches)
    }

    @Test
    fun `created folders appear in the content`() = runTest {
        repository.createFolder(PlatformFile(root), "Chor")

        assertEquals(listOf("Chor"), repository.content(PlatformFile(root)).items.map { it.displayName })
    }

    @Test
    fun `imported images are stored in order with a manifest and a thumbnail`() = runTest {
        val second = image("b.JPG", "jpg", width = 1200, height = 1700)
        val first = image("a.png", "png", width = 800, height = 1100)
        val metadata = ScoreMetadata(title = "Etude", composer = "Chopin")

        importImages(metadata, second, first)

        val score = File(root, "Etude.citscore")
        val manifest = AppJson.decodeFromString<ScoreManifest>(File(score, CitScore.MANIFEST_FILE).readText())
        assertEquals(metadata, manifest.metadata)
        assertEquals(now, manifest.createdAt)
        assertNull(manifest.lastOpenedAt)
        val part = manifest.parts.single()
        assertTrue(part.directory.startsWith("${CitScore.PARTS_DIRECTORY}/"))
        val pages = (part.content as ScoreContent.Pages).pages
        assertTrue(pages.all { it.source.startsWith("${CitScore.SOURCES_DIRECTORY}/") && it.page == 0 && it.normalization.image.startsWith("${CitScore.PAGES_DIRECTORY}/") })
        assertEquals(listOf("jpg", "png"), pages.map { it.source.substringAfterLast('.') })
        assertEquals(listOf(PageCrop(0f, 0f, 1200f, 1700f), PageCrop(0f, 0f, 800f, 1100f)), pages.map { it.crop })
        val partDirectory = File(score, part.directory)
        assertContentEquals(second.readBytes(), File(partDirectory, pages[0].source).readBytes())
        assertContentEquals(first.readBytes(), File(partDirectory, pages[1].source).readBytes())
    }

    @Test
    fun `pages keep the original next to the black and white image`() = runTest {
        val photo = image("photo.jpg", "jpg", width = 400, height = 500)
        val cleaned = File(root, "scan.png").also { ImageIO.write(sheet(400, 500, 100, 100, 300, 400), "png", it) }.readBytes()
        val crop = PageCrop(90f, 90f, 310f, 410f)
        val erased = listOf(EraserStroke(listOf(Offset(1f, 2f), Offset(3f, 4f)), 5f))
        val page = NewPage(PageOrigin.File(PlatformFile(photo)), 0, crop, NewNormalization(cleaned, 0.2f, erased))

        repository.importScore(PlatformFile(root), ScoreMetadata(title = "Etude"), listOf(NewPart("", PartSource.Pages(listOf(page)))))

        val score = File(root, "Etude.citscore")
        val part = repository.open(PlatformFile(score)).parts.single()
        val stored = (part.content as ScoreContent.Pages).pages.single()
        val normalization = stored.normalization
        assertEquals(crop to (0.2f to erased), stored.crop to (normalization.threshold to normalization.erased))
        assertContentEquals(photo.readBytes(), File(score, "${part.directory}/${stored.source}").readBytes())
        assertContentEquals(cleaned, File(score, "${part.directory}/${normalization.image}").readBytes())
        assertEquals(listOf(PartDisplay.Original, PartDisplay.Normalized), part.displays)
        part.displays.forEach { display ->
            ScoreRenderers.open(PartRef(PlatformFile(score), part.directory).root, part.content, display).use { assertEquals(listOf(crop.size), it.pageSizes) }
        }
    }

    @Test
    fun `the original display maps the black and white crop onto the original page`() = runTest {
        val photo = File(root, "photo.png").also { ImageIO.write(sheet(500, 500, 0, 0, 250, 500), "png", it) }
        val cleaned = image("scan.png", "png", width = SCAN_PAGE_WIDTH.toInt(), height = SCAN_PAGE_WIDTH.toInt(), color = Color.WHITE).readBytes()
        val crop = PageCrop(0f, 0f, SCAN_PAGE_WIDTH / 4, SCAN_PAGE_WIDTH)
        val page = NewPage(PageOrigin.File(PlatformFile(photo)), 0, crop, NewNormalization(cleaned, 0.2f, emptyList()))

        repository.importScore(PlatformFile(root), ScoreMetadata(title = "Etude"), listOf(NewPart("", PartSource.Pages(listOf(page)))))

        val part = firstPart("Etude")
        val content = repository.load(part.score).parts.single().content
        val region = Rect(Offset.Zero, crop.size)
        val original = ScoreRenderers.open(part.root, content, PartDisplay.Original).use { it.render(0, region, IntSize(10, 40)) }
        val normalized = ScoreRenderers.open(part.root, content, PartDisplay.Normalized).use { it.render(0, region, IntSize(10, 40)) }
        assertColorNear(Color.BLACK, Color(original.toAwtImage().getRGB(5, 20)))
        assertColorNear(Color.WHITE, Color(normalized.toAwtImage().getRGB(5, 20)))
    }

    @Test
    fun `the chosen display is device state`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val part = firstPart("Etude")

        repository.saveDisplay(part, PartDisplay.Normalized)

        val shared = File(root, "Etude.citscore/${CitScore.MANIFEST_FILE}").readText()
        assertFalse("display" in shared, shared)
        assertEquals(PartDisplay.Normalized, repository.load(part.score).parts.single().shownDisplay)
    }

    @Test
    fun `recognized music is stored with the part and removed again`() = runTest {
        val layout = RecognitionLayout(
            notes = listOf(RecognizedNote("p1n0", 0, 1, 10f, 20f, 14f, 24f)),
            measures = listOf(RecognizedMeasure(0, 1, 5f, 15f, 100f, 60f, 4f)),
        )
        val music = RecognizedMusic("<score-partwise/>", layout)

        repository.importScore(PlatformFile(root), ScoreMetadata(title = "Etude"), listOf(imagePart("", image("a.png", "png", 100, 140)).copy(recognition = music)))

        val score = PlatformFile(File(root, "Etude.citscore"))
        val part = repository.open(score).parts.single()
        val stored = assertNotNull(repository.recognition(PartRef(score, part.directory), assertNotNull(part.recognition)))
        assertEquals(music.musicXml, stored.musicXml)
        assertEquals(layout, stored.layout)

        repository.updateScore(PlatformFile(root), score, ScoreMetadata(title = "Etude"), listOf(PartEdit.Keep(part.directory, "", recognition = RecognitionEdit.Remove)))

        assertNull(repository.open(score).parts.single().recognition)
        assertFalse(File(root, "Etude.citscore/${part.directory}/${CitScore.RECOGNITION_MUSICXML_FILE}").exists())
        assertFalse(File(root, "Etude.citscore/${part.directory}/${CitScore.RECOGNITION_LAYOUT_FILE}").exists())
    }

    @Test
    fun `audio attachments are copied, renamed and deleted`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val score = File(root, "Etude.citscore")
        val source = File(root, "take.wav").apply { writeBytes(byteArrayOf(1, 2, 3)) }

        val attachment = repository.addAttachment(PlatformFile(score), AttachmentKind.Audio, PlatformFile(source), "Take")
        repository.renameAttachment(PlatformFile(score), AttachmentKind.Audio, attachment, "Final take")

        val renamed = attachment.copy(name = "Final take")
        assertEquals(listOf(renamed), repository.attachments(PlatformFile(score), AttachmentKind.Audio))
        val stored = File(score, attachment.path)
        assertContentEquals(source.readBytes(), stored.readBytes())

        repository.deleteAttachment(PlatformFile(score), AttachmentKind.Audio, renamed)

        assertEquals(emptyList(), repository.attachments(PlatformFile(score), AttachmentKind.Audio))
        assertFalse(stored.exists())
    }

    @Test
    fun `editing an image score reorders, removes, adds pages and renames it`() = runTest {
        val first = image("a.png", "png", width = 100, height = 140)
        val second = image("b.png", "png", width = 200, height = 280)
        val third = image("c.png", "png", width = 300, height = 420)
        importImages(ScoreMetadata(title = "Etude"), first, second)
        val score = PlatformFile(File(root, "Etude.citscore"))
        val part = firstPart("Etude")
        val audio = repository.addAttachment(score, AttachmentKind.Audio, PlatformFile(first), "Take")
        val before = repository.load(score).parts.single().content as ScoreContent.Pages

        val renamed = repository.updateScore(
            folder = PlatformFile(root),
            score = score,
            metadata = ScoreMetadata(title = "Studie", composer = "Czerny"),
            edits = listOf(
                PartEdit.Keep(
                    part.directory,
                    "Violine",
                    PartSource.Pages(listOf(NewPage(PageOrigin.File(PlatformFile(third)), 0, PageCrop(0f, 0f, 300f, 420f), normalization(PlatformFile(third))), before.kept(part, 1))),
                ),
            ),
        )

        assertFalse(File(root, "Etude.citscore").exists())
        val directory = File(root, "Studie.citscore/${part.directory}")
        assertTrue(directory.exists())
        val manifest = repository.load(renamed)
        assertEquals(ScoreMetadata(title = "Studie", composer = "Czerny"), manifest.metadata)
        val edited = manifest.parts.single()
        assertEquals("Violine", edited.name)
        assertEquals(listOf(audio), manifest.audio)
        assertContentEquals(first.readBytes(), File(root, "Studie.citscore/${audio.path}").readBytes())
        val pages = (edited.content as ScoreContent.Pages).pages
        assertEquals(listOf(PageCrop(0f, 0f, 300f, 420f), before.pages[1].crop), pages.map { it.crop })
        assertEquals(before.pages[1].source to before.pages[1].page, pages[1].source to pages[1].page)
        assertContentEquals(third.readBytes(), File(directory, pages[0].source).readBytes())
        assertFalse(File(directory, before.pages[0].source).exists())
    }

    @Test
    fun `editing a pdf score reorders and removes pages`() = runTest {
        val pdf = PdfSourceTest.samplePdf(root)
        val crops = openPdfSource(PlatformFile(pdf)).use { source -> source.pageSizes.indices.map { source.contentCrop(it) } }
        importPdf("Sonate", pdf, crops)
        val score = PlatformFile(File(root, "Sonate.citscore"))
        val part = firstPart("Sonate")

        val before = repository.load(score).parts.single().content as ScoreContent.Pages

        repository.updateScore(PlatformFile(root), score, ScoreMetadata(title = "Sonate"), listOf(PartEdit.Keep(part.directory, "", PartSource.Pages(listOf(before.kept(part, 1))))))

        val content = repository.load(score).parts.single().content as ScoreContent.Pages
        assertEquals(listOf(Triple(before.pages[0].source, 1, crops[1])), content.pages.map { Triple(it.source, it.page, it.crop) })
        ScoreRenderers.open(part.root, content, PartDisplay.Original).use { renderer -> assertEquals(listOf(crops[1].size), renderer.pageSizes) }
    }

    @Test
    fun `annotations are stored next to the manifest and follow edited pages`() = runTest {
        importImages(ScoreMetadata(title = "Etude"), image("a.png", "png", 100, 140), image("b.png", "png", 100, 140))
        val score = PlatformFile(File(root, "Etude.citscore"))
        val part = firstPart("Etude")
        val original = DisplayRef(part, PartDisplay.Original)
        val normalized = DisplayRef(part, PartDisplay.Normalized)
        val onFirst = InkStroke(page = 0, points = listOf(1f, 2f, 3f, 4f), width = 0.5f)
        val onSecond = TextNote(page = 1, x = 10f, y = 20f, text = "p", size = 3f)

        assertEquals(ScoreAnnotations(), repository.annotations(original))
        repository.saveAnnotations(original, ScoreAnnotations(listOf(onFirst), listOf(onSecond)))
        repository.saveAnnotations(normalized, ScoreAnnotations(listOf(onFirst.copy(page = 1))))
        assertTrue(File(root, "Etude.citscore/${part.directory}/${CitScore.annotationsFile(PartDisplay.Original)}").exists())
        assertEquals(ScoreAnnotations(), repository.annotations(DisplayRef(part, PartDisplay.MusicXml)))

        val before = repository.load(score).parts.single().content as ScoreContent.Pages
        repository.updateScore(PlatformFile(root), score, ScoreMetadata(title = "Etude"), listOf(PartEdit.Keep(part.directory, "", PartSource.Pages(listOf(before.kept(part, 1))))))

        assertEquals(ScoreAnnotations(texts = listOf(onSecond.copy(page = 0))), repository.annotations(original))
        assertEquals(ScoreAnnotations(listOf(onFirst.copy(page = 0))), repository.annotations(normalized))
    }

    @Test
    fun `any file can be attached and exported again`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val score = File(root, "Etude.citscore")
        val part = PlatformFile(score)
        val musescore = File(root, "Etude.mscz").apply { writeBytes(byteArrayOf(9, 8, 7)) }
        val withoutExtension = File(root, "LIESMICH").apply { writeText("notes") }

        val first = repository.addAttachment(part, AttachmentKind.File, PlatformFile(musescore), musescore.name)
        val second = repository.addAttachment(part, AttachmentKind.File, PlatformFile(withoutExtension), withoutExtension.name)

        assertEquals(listOf("Etude.mscz", "LIESMICH"), repository.attachments(part, AttachmentKind.File).map { it.name })
        assertEquals(emptyList(), repository.attachments(part, AttachmentKind.Audio))
        val exported = File(root, "export.mscz")
        repository.exportAttachment(part, first, PlatformFile(exported))
        assertContentEquals(musescore.readBytes(), exported.readBytes())

        repository.deleteAttachment(part, AttachmentKind.File, second)

        assertEquals(listOf(first), repository.attachments(part, AttachmentKind.File))
        assertFalse(File(score, second.path).exists())
    }

    @Test
    fun `thumbnail stacks all pages with a shared scale`() = runTest {
        val firstLine = image("1.png", "png", width = 1200, height = 600, color = Color.RED)
        val secondLine = image("2.png", "png", width = 2400, height = 1200, color = Color.BLUE)
        importImages(ScoreMetadata(title = "Etude"), firstLine, secondLine)

        val thumbnail = assertNotNull(repository.thumbnail(PlatformFile(File(root, "Etude.citscore"))))

        val decoded = ImageIO.read(thumbnail.inputStream())
        assertEquals(CitScore.THUMBNAIL_WIDTH, decoded.width)
        assertEquals(849, decoded.height)
        assertColorNear(Color.RED, Color(decoded.getRGB(100, 80)))
        assertColorNear(Color.WHITE, Color(decoded.getRGB(500, 80)))
        assertColorNear(Color.BLUE, Color(decoded.getRGB(300, 300)))
        assertColorNear(Color.WHITE, Color(decoded.getRGB(300, 700)))
    }

    @Test
    fun `thumbnail is missing for incomplete scores`() = runTest {
        val incomplete = File(root, "Incomplete.citscore").apply { mkdir() }

        assertNull(repository.thumbnail(PlatformFile(incomplete)))
    }

    @Test
    fun `opening a score records the time and returns its manifest`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        now = Instant.parse("2026-02-01T00:00:00Z")

        val manifest = repository.open(PlatformFile(File(root, "Etude.citscore")))

        assertEquals("Etude", manifest.metadata.title)
        assertEquals(now, manifest.lastOpenedAt)
        assertEquals(now, (repository.content(PlatformFile(root)).items.single() as LibraryItem.Score).lastActivity)
    }

    @Test
    fun `view state and selected part stay on the device`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val score = File(root, "Etude.citscore")
        val file = PlatformFile(score)
        val opened = repository.open(file)
        val part = PartRef(file, opened.parts.single().directory)

        repository.saveView(part, PartDisplay.Original, ScoreViewState(2f, 0.5f, 0.25f))
        repository.saveView(part, PartDisplay.MusicXml, ScoreViewState(1.5f, 0f, 0.75f))
        repository.selectPart(part)
        repository.saveScrollMode(file, ScrollMode.Both)

        val shared = File(score, CitScore.MANIFEST_FILE).readText()
        val device = AppJson.decodeFromString<ScoreDeviceState>(File(score, CitScore.DEVICE_STATE_FILE).readText())
        assertFalse("lastOpenedAt" in shared || "view" in shared || "currentPart" in shared || "scrollMode" in shared, shared)
        assertEquals(
            ScoreDeviceState(now, part.directory, ScrollMode.Both, mapOf(part.directory to ScoreViewState(2f, 0.5f, 0.25f)), mapOf(part.directory to ScoreViewState(1.5f, 0f, 0.75f))),
            device,
        )
    }

    @Test
    fun `saving the view keeps the rest of the manifest`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val score = PlatformFile(File(root, "Etude.citscore"))
        val opened = repository.open(score)
        val part = opened.parts.single()

        repository.saveView(PartRef(score, part.directory), PartDisplay.Normalized, ScoreViewState(zoom = 1.5f, x = 0.25f, y = 0.5f))

        assertEquals(
            opened.updatePart(part.directory) { it.copy(view = ScoreViewState(1.5f, 0.25f, 0.5f)) },
            repository.open(score).copy(lastOpenedAt = opened.lastOpenedAt),
        )
    }

    @Test
    fun `tempo and instrument stay on the device`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val score = PlatformFile(File(root, "Etude.citscore"))
        val part = repository.open(score).parts.single()
        val playback = PlaybackPreferences(tempo = 0.75f, instrument = Instrument.Violin)

        repository.savePlayback(PartRef(score, part.directory), playback)

        val shared = File(root, "Etude.citscore/${CitScore.MANIFEST_FILE}").readText()
        val device = AppJson.decodeFromString<ScoreDeviceState>(File(root, "Etude.citscore/${CitScore.DEVICE_STATE_FILE}").readText())
        assertFalse("playback" in shared, shared)
        assertEquals(mapOf(part.directory to playback), device.playback)
        assertEquals(playback, repository.load(score).parts.single().playback)
    }

    @Test
    fun `imported pdfs keep the original file and store page crops`() = runTest {
        val pdf = PdfSourceTest.samplePdf(root)
        val crops = openPdfSource(PlatformFile(pdf)).use { source -> source.pageSizes.indices.map { source.contentCrop(it) } }

        importPdf("Sonate", pdf, crops)

        val score = File(root, "Sonate.citscore")
        val part = repository.open(PlatformFile(score)).parts.single()
        val pages = (part.content as ScoreContent.Pages).pages
        assertEquals(1, pages.map { it.source }.distinct().size)
        assertContentEquals(pdf.readBytes(), File(score, "${part.directory}/${pages[0].source}").readBytes())
        assertEquals(crops.mapIndexed { index, crop -> Triple(pages[0].source, index, crop) }, pages.map { Triple(it.source, it.page, it.crop) })
        assertNotNull(repository.thumbnail(PlatformFile(score)))
        ScoreRenderers.open(PartRef(PlatformFile(score), part.directory).root, part.content, PartDisplay.Original).use { renderer ->
            assertEquals(crops.map { it.size }, renderer.pageSizes)
        }
    }

    @Test
    fun `exported scores contain the score directory without the device state`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val score = PlatformFile(File(root, "Etude.citscore"))
        repository.open(score)
        val target = File(root, "Etude.citscore.zip")

        repository.exportScore(score, PlatformFile(target))

        val entries = ZipFile(target).use { zip -> zip.entries().toList().associate { it.name to zip.getInputStream(it).readBytes() } }
        val expected = File(root, "Etude.citscore").walkTopDown().filter { it.isFile && it.name != CitScore.DEVICE_STATE_FILE }
            .associate { "Etude.citscore/${it.relativeTo(File(root, "Etude.citscore")).invariantSeparatorsPath}" to it.readBytes() }
        assertEquals(expected.keys, entries.keys)
        expected.forEach { (name, content) -> assertContentEquals(content, entries.getValue(name)) }
    }

    @Test
    fun `deleting removes scores and folders with their content`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val folder = File(root, "Chor").apply { mkdir() }
        File(folder, "Sub").mkdir()
        File(folder, "notes.txt").writeText("text")

        repository.delete(PlatformFile(File(root, "Etude.citscore")))
        repository.delete(PlatformFile(folder))

        assertTrue(repository.content(PlatformFile(root)).isEmpty)
        assertFalse(folder.exists())
    }

    @Test
    fun `failed imports leave no partial score behind`() = runTest {
        val notAnImage = File(root, "broken.png").apply { writeText("not an image") }

        val part = NewPart("", PartSource.Pages(listOf(NewPage(PageOrigin.File(PlatformFile(notAnImage)), 0, PageCrop(0f, 0f, 1f, 1f), NewNormalization(ByteArray(0), 0.2f, emptyList())))))
        runCatching { repository.importScore(PlatformFile(root), ScoreMetadata(title = "Etude"), listOf(part)) }

        assertFalse(File(root, "Etude.citscore").exists())
    }

    @Test
    fun `scores store every part in its own directory and take the thumbnail from the first`() = runTest {
        val violin = image("violin.png", "png", width = 1200, height = 600, color = Color.RED)
        val piano = image("piano.png", "png", width = 1200, height = 600, color = Color.BLUE)

        repository.importScore(PlatformFile(root), ScoreMetadata("Sonate"), listOf(imagePart("Violine", violin), imagePart("Klavier", piano)))

        val score = File(root, "Sonate.citscore")
        val parts = repository.load(PlatformFile(score)).parts
        assertEquals(listOf("Violine", "Klavier"), parts.map { it.name })
        assertEquals(2, parts.map { it.directory }.toSet().size)
        parts.zip(listOf(violin, piano)).forEach { (part, image) ->
            val source = (part.content as ScoreContent.Pages).pages.single().source
            assertContentEquals(image.readBytes(), File(score, "${part.directory}/$source").readBytes())
        }
        val thumbnail = ImageIO.read(assertNotNull(repository.thumbnail(PlatformFile(score))).inputStream())
        assertColorNear(Color.RED, Color(thumbnail.getRGB(100, 80)))
    }

    @Test
    fun `editing parts adds, reorders, renames and deletes them`() = runTest {
        val folder = PlatformFile(root)
        repository.importScore(folder, ScoreMetadata("Duett"), listOf(imagePart("Violine", image("v.png", "png", 50, 70))))
        val score = File(root, "Duett.citscore")
        val file = PlatformFile(score)
        val first = repository.load(file).parts.single()
        repository.updateScore(folder, file, ScoreMetadata("Duett"), listOf(PartEdit.Keep(first.directory, "Violine"), PartEdit.Add(imagePart("Klavier", image("p.png", "png", 50, 70)))))
        val added = repository.load(file).parts[1]
        repository.selectPart(PartRef(file, added.directory))

        repository.updateScore(folder, file, ScoreMetadata("Duett"), listOf(PartEdit.Keep(added.directory, "Piano"), PartEdit.Add(imagePart("Flöte", image("f.png", "png", 50, 70)))))

        val manifest = repository.load(file)
        assertEquals(listOf("Piano", "Flöte"), manifest.parts.map { it.name })
        assertEquals(added.directory, manifest.currentPart)
        assertFalse(File(score, first.directory).exists())
        assertTrue(File(score, added.directory).exists())

        repository.updateScore(folder, file, ScoreMetadata("Duett"), listOf(PartEdit.Keep(manifest.parts[1].directory, "Flöte")))

        assertNull(repository.load(file).currentPart)
        assertFalse(File(score, added.directory).exists())
    }

    @Test
    fun `musicxml parts keep the original file and are engraved for the thumbnail`() = runTest {
        FileKit.init(appId = "citrinscore-test", filesDir = File(root, ".files"))
        val musicXml = File(root, "Walzer.MXL").apply { writeBytes(MusicXmlEngraverTest.mxl(MusicXmlEngraverTest.score(measures = 2))) }

        repository.importScore(PlatformFile(root), ScoreMetadata("Walzer"), listOf(NewPart("Klavier", PartSource.MusicXml(PlatformFile(musicXml)))))

        val score = File(root, "Walzer.citscore")
        val part = repository.load(PlatformFile(score)).parts.single()
        assertEquals(ScoreContent.MusicXml("score.mxl"), part.content)
        assertContentEquals(musicXml.readBytes(), File(score, "${part.directory}/score.mxl").readBytes())
        assertNotNull(repository.thumbnail(PlatformFile(score)))
    }

    private suspend fun importImages(metadata: ScoreMetadata, vararg images: File) {
        repository.importScore(PlatformFile(root), metadata, listOf(imagePart("", *images)))
    }

    private suspend fun imagePart(name: String, vararg images: File): NewPart {
        val files = images.map(::PlatformFile)
        return NewPart(name, PartSource.Pages(files.map { file -> NewPage(PageOrigin.File(file), 0, openImageSource(file).use { it.contentCrop(0) }, normalization(file)) }))
    }

    private suspend fun importPdf(title: String, pdf: File, crops: List<PageCrop>) {
        val pages = crops.mapIndexed { index, crop -> NewPage(PageOrigin.File(PlatformFile(pdf)), index, crop, normalization(PlatformFile(pdf), index)) }
        repository.importScore(PlatformFile(root), ScoreMetadata(title), listOf(NewPart("", PartSource.Pages(pages))))
    }

    private suspend fun ScoreContent.Pages.kept(part: PartRef, index: Int) = pages[index].let { page ->
        val image = CitScore.resolve(part.root, page.normalization.image).readBytes()
        NewPage(PageOrigin.Stored(page.source), page.page, page.crop, NewNormalization(image, page.normalization.threshold, page.normalization.erased), previousIndex = index)
    }

    private suspend fun normalization(file: PlatformFile, page: Int = 0) = openPageSource(file).use { source ->
        val size = source.pageSizes[page]
        val image = source.render(page, Rect(Offset.Zero, size), IntSize(size.width.roundToInt(), size.height.roundToInt()))
        NewNormalization(image.encodeToByteArray(ImageFormat.PNG), DEFAULT_SCAN_THRESHOLD, emptyList())
    }

    private suspend fun firstPart(title: String): PartRef {
        val score = PlatformFile(File(root, "$title.citscore"))
        return PartRef(score, repository.load(score).parts.first().directory)
    }

    private suspend fun importAt(title: String, time: Instant) {
        now = time
        val page = image("$title.png", "png", width = 10, height = 14)
        importImages(ScoreMetadata(title), page)
        page.delete()
    }

    private fun score(title: String, createdAt: String, lastOpenedAt: String) {
        val manifest = ScoreManifest(
            metadata = ScoreMetadata(title),
            parts = listOf(ScorePart("parts/a", "", ScoreContent.Pages(emptyList()))),
            createdAt = Instant.parse(createdAt),
            lastOpenedAt = Instant.parse(lastOpenedAt),
        )
        File(root, "$title.citscore").apply { mkdir() }.resolve(CitScore.MANIFEST_FILE).writeText(AppJson.encodeToString(manifest))
    }

    private fun image(name: String, format: String, width: Int, height: Int, color: Color = Color.BLACK): File {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply {
            this.color = color
            fillRect(0, 0, width, height)
            dispose()
        }
        return File(root, name).also { ImageIO.write(image, format, it) }
    }

    private fun sheet(width: Int, height: Int, inkLeft: Int, inkTop: Int, inkRight: Int, inkBottom: Int) =
        BufferedImage(width, height, BufferedImage.TYPE_INT_RGB).apply {
            createGraphics().apply {
                color = Color.WHITE
                fillRect(0, 0, width, height)
                color = Color.BLACK
                fillRect(inkLeft, inkTop, inkRight - inkLeft, inkBottom - inkTop)
                fillRect(50, 50, 2, 2)
                dispose()
            }
        }

    private fun assertColorNear(expected: Color, actual: Color) {
        val distance = maxOf(abs(expected.red - actual.red), abs(expected.green - actual.green), abs(expected.blue - actual.blue))
        assertTrue(distance <= 16, "expected $expected but was $actual")
    }
}
