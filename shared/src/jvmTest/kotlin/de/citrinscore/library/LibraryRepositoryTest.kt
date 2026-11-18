package de.citrinscore.library

import de.citrinscore.score.Attachment
import de.citrinscore.score.AttachmentKind
import de.citrinscore.score.CitScore
import de.citrinscore.score.ImagePage
import de.citrinscore.score.InkStroke
import de.citrinscore.score.PageCrop
import de.citrinscore.score.PdfPage
import de.citrinscore.score.PlaybackPreferences
import de.citrinscore.score.playback.Instrument
import de.citrinscore.score.ScoreAnnotations
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreDeviceState
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
        val pages = (part.content as ScoreContent.Images).pages
        assertEquals(listOf("pages/001.jpg", "pages/002.png"), pages.map { it.path })
        assertEquals(listOf(PageCrop(0f, 0f, 1200f, 1700f), PageCrop(0f, 0f, 800f, 1100f)), pages.map { it.crop })
        assertEquals(listOf(emptyList(), emptyList<Float>()), part.systemBreaks)
        val partDirectory = File(score, part.directory)
        assertContentEquals(second.readBytes(), File(partDirectory, "pages/001.jpg").readBytes())
        assertContentEquals(first.readBytes(), File(partDirectory, "pages/002.png").readBytes())
    }

    @Test
    fun `imports store the system breaks relative to the cropped page`() = runTest {
        val page = File(root, "systems.png")
        ImageIO.write(
            BufferedImage(1000, 1000, BufferedImage.TYPE_INT_RGB).apply {
                createGraphics().apply {
                    color = Color.WHITE
                    fillRect(0, 0, 1000, 1000)
                    color = Color.BLACK
                    fillRect(100, 100, 800, 200)
                    fillRect(100, 600, 800, 200)
                    dispose()
                }
            },
            "png",
            page,
        )

        importImages(ScoreMetadata(title = "Etude"), page)

        val manifest = repository.open(PlatformFile(File(root, "Etude.citscore")))
        assertEquals(360f, assertNotNull(manifest.parts.single().systemBreaks).single().single(), absoluteTolerance = 0.5f)
    }

    @Test
    fun `saving the system breaks keeps the rest of the manifest`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val score = PlatformFile(File(root, "Etude.citscore"))
        val opened = repository.open(score)
        val part = opened.parts.single()

        repository.saveSystemBreaks(PartRef(score, part.directory), listOf(listOf(4f)))

        assertEquals(
            opened.updatePart(part.directory) { it.copy(systemBreaks = listOf(listOf(4f))) },
            repository.open(score).copy(lastOpenedAt = opened.lastOpenedAt),
        )
    }

    @Test
    fun `imported scans are stored as png pages`() = runTest {
        val scan = File(root, "scan.png").also { ImageIO.write(sheet(400, 500, 100, 100, 300, 400), "png", it) }.readBytes()
        val crop = PageCrop(90f, 90f, 310f, 410f)

        repository.importScore(PlatformFile(root), ScoreMetadata(title = "Etude"), listOf(NewPart("", PartSource.Scans(listOf(scan, scan), listOf(crop, crop)))))

        val score = File(root, "Etude.citscore")
        val part = repository.open(PlatformFile(score)).parts.single()
        assertEquals(ScoreContent.Images(listOf(ImagePage("pages/001.png", crop), ImagePage("pages/002.png", crop))), part.content)
        assertContentEquals(scan, File(score, "${part.directory}/pages/002.png").readBytes())
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
        val before = repository.load(score).parts.single().content as ScoreContent.Images

        val renamed = repository.updateScore(
            folder = PlatformFile(root),
            score = score,
            metadata = ScoreMetadata(title = "Studie", composer = "Czerny"),
            edits = listOf(
                PartEdit.Keep(part.directory, "Violine", listOf(PageEdit.Add(PlatformFile(third), PageCrop(0f, 0f, 300f, 420f)), PageEdit.Keep(1))),
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
        val pages = (edited.content as ScoreContent.Images).pages
        assertEquals(listOf(PageCrop(0f, 0f, 300f, 420f), before.pages[1].crop), pages.map { it.crop })
        assertEquals(before.pages[1], pages[1])
        assertContentEquals(third.readBytes(), File(directory, pages[0].path).readBytes())
        assertFalse(File(directory, before.pages[0].path).exists())
        assertEquals(2, edited.systemBreaks?.size)
    }

    @Test
    fun `editing a pdf score reorders and removes pages`() = runTest {
        val pdf = PdfSourceTest.samplePdf(root)
        val crops = openPdfSource(PlatformFile(pdf)).use { source -> source.pageSizes.indices.map { source.contentCrop(it) } }
        importPdf("Sonate", pdf, crops)
        val score = PlatformFile(File(root, "Sonate.citscore"))
        val part = firstPart("Sonate")

        repository.updateScore(PlatformFile(root), score, ScoreMetadata(title = "Sonate"), listOf(PartEdit.Keep(part.directory, "", listOf(PageEdit.Keep(1)))))

        val content = repository.load(score).parts.single().content
        assertEquals(ScoreContent.Pdf(CitScore.PDF_FILE, listOf(PdfPage(1, crops[1]))), content)
        ScoreRenderers.open(part.root, content).use { renderer -> assertEquals(listOf(crops[1].size), renderer.pageSizes) }
    }

    @Test
    fun `annotations are stored next to the manifest and follow edited pages`() = runTest {
        importImages(ScoreMetadata(title = "Etude"), image("a.png", "png", 100, 140), image("b.png", "png", 100, 140))
        val score = PlatformFile(File(root, "Etude.citscore"))
        val part = firstPart("Etude")
        val onFirst = InkStroke(page = 0, points = listOf(1f, 2f, 3f, 4f), width = 0.5f)
        val onSecond = TextNote(page = 1, x = 10f, y = 20f, text = "p", size = 3f)

        assertEquals(ScoreAnnotations(), repository.annotations(part))
        repository.saveAnnotations(part, ScoreAnnotations(listOf(onFirst), listOf(onSecond)))
        assertTrue(File(root, "Etude.citscore/${part.directory}/${CitScore.ANNOTATIONS_FILE}").exists())

        repository.updateScore(PlatformFile(root), score, ScoreMetadata(title = "Etude"), listOf(PartEdit.Keep(part.directory, "", listOf(PageEdit.Keep(1)))))

        assertEquals(ScoreAnnotations(texts = listOf(onSecond.copy(page = 0))), repository.annotations(part))
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
    fun `opening a legacy score computes the missing crops`() = runTest {
        val score = File(root, "Legacy.citscore").apply { mkdir() }
        val pages = File(score, "pages").apply { mkdir() }
        ImageIO.write(sheet(1000, 1400, inkLeft = 200, inkTop = 300, inkRight = 800, inkBottom = 1100), "png", File(pages, "001.png"))
        File(score, CitScore.MANIFEST_FILE).writeText(
            """{"metadata":{"title":"Legacy"},"content":{"type":"images","pages":["pages/001.png"]},"createdAt":"2025-01-01T00:00:00Z"}""",
        )

        val manifest = repository.open(PlatformFile(score))

        val expected = ImagePage("pages/001.png", PageCrop(190f, 290f, 810f, 1110f))
        val part = manifest.parts.single()
        assertEquals("", part.directory)
        assertEquals(ScoreContent.Images(listOf(expected)), part.content)
        assertEquals(manifest.shared, AppJson.decodeFromString<ScoreManifest>(File(score, CitScore.MANIFEST_FILE).readText()))
    }

    @Test
    fun `view state stays on the device and legacy view state is moved there`() = runTest {
        val score = File(root, "Legacy.citscore").apply { mkdir() }
        File(score, "score.musicxml").writeText(MusicXmlEngraverTest.score(measures = 1))
        File(score, CitScore.MANIFEST_FILE).writeText(
            """{"metadata":{"title":"Legacy"},"content":{"type":"musicxml","path":"score.musicxml"},"view":{"zoom":2,"x":0.5,"y":0.25},"createdAt":"2025-01-01T00:00:00Z"}""",
        )
        val file = PlatformFile(score)

        val opened = repository.open(file)
        repository.selectPart(PartRef(file, ""))

        val shared = File(score, CitScore.MANIFEST_FILE).readText()
        val device = AppJson.decodeFromString<ScoreDeviceState>(File(score, CitScore.DEVICE_STATE_FILE).readText())
        assertFalse("lastOpenedAt" in shared || "view" in shared || "currentPart" in shared, shared)
        assertEquals(ScoreDeviceState(now, "", mapOf("" to ScoreViewState(2f, 0.5f, 0.25f))), device)
        assertEquals(opened.copy(currentPart = ""), repository.load(file))
    }

    @Test
    fun `saving the view keeps the rest of the manifest`() = runTest {
        importAt("Etude", Instant.parse("2026-01-01T00:00:00Z"))
        val score = PlatformFile(File(root, "Etude.citscore"))
        val opened = repository.open(score)
        val part = opened.parts.single()

        repository.saveView(PartRef(score, part.directory), ScoreViewState(zoom = 1.5f, x = 0.25f, y = 0.5f))

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
        assertContentEquals(pdf.readBytes(), File(score, "${part.directory}/${CitScore.PDF_FILE}").readBytes())
        assertEquals(ScoreContent.Pdf(CitScore.PDF_FILE, crops.mapIndexed(::PdfPage)), part.content)
        assertNotNull(repository.thumbnail(PlatformFile(score)))
        ScoreRenderers.open(PartRef(PlatformFile(score), part.directory).root, part.content).use { renderer ->
            assertEquals(crops.map { it.size }, renderer.pageSizes)
        }
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

        val part = NewPart("", PartSource.Images(listOf(PlatformFile(notAnImage)), listOf(PageCrop(0f, 0f, 1f, 1f))))
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
        assertContentEquals(violin.readBytes(), File(score, "${parts[0].directory}/pages/001.png").readBytes())
        assertContentEquals(piano.readBytes(), File(score, "${parts[1].directory}/pages/001.png").readBytes())
        val thumbnail = ImageIO.read(assertNotNull(repository.thumbnail(PlatformFile(score))).inputStream())
        assertColorNear(Color.RED, Color(thumbnail.getRGB(100, 80)))
    }

    @Test
    fun `editing parts adds, reorders, renames and deletes them`() = runTest {
        val score = File(root, "Legacy.citscore").apply { mkdir() }
        File(score, "pages").mkdir()
        ImageIO.write(sheet(100, 140, 10, 10, 90, 130), "png", File(score, "pages/001.png"))
        File(score, CitScore.ANNOTATIONS_FILE).writeText("{}")
        File(score, "audio").mkdir()
        File(score, "audio/a.wav").writeText("take")
        File(score, CitScore.MANIFEST_FILE).writeText(
            """{"metadata":{"title":"Legacy"},"content":{"type":"images","pages":["pages/001.png"]},"createdAt":"2025-01-01T00:00:00Z",
               "audio":[{"path":"audio/a.wav","name":"Take"}]}""",
        )
        val folder = PlatformFile(root)
        val file = PlatformFile(score)
        repository.updateScore(folder, file, ScoreMetadata("Legacy"), listOf(PartEdit.Keep("", "Violine"), PartEdit.Add(imagePart("Klavier", image("p.png", "png", 50, 70)))))
        val added = repository.load(file).parts[1]
        repository.selectPart(PartRef(file, added.directory))

        repository.updateScore(folder, file, ScoreMetadata("Legacy"), listOf(PartEdit.Keep(added.directory, "Piano"), PartEdit.Add(imagePart("Flöte", image("f.png", "png", 50, 70)))))

        val manifest = repository.load(file)
        assertEquals(listOf("Piano", "Flöte"), manifest.parts.map { it.name })
        assertEquals(added.directory, manifest.currentPart)
        assertFalse(File(score, "pages").exists())
        assertFalse(File(score, CitScore.ANNOTATIONS_FILE).exists())
        assertEquals(listOf(Attachment("audio/a.wav", "Take")), manifest.audio)
        assertEquals("take", File(score, "audio/a.wav").readText())
        assertTrue(File(score, added.directory).exists())

        repository.updateScore(folder, file, ScoreMetadata("Legacy"), listOf(PartEdit.Keep(manifest.parts[1].directory, "Flöte")))

        assertNull(repository.load(file).currentPart)
        assertFalse(File(score, added.directory).exists())
    }

    @Test
    fun `attachments stored in a deleted part move to the score`() = runTest {
        val score = File(root, "Duett.citscore").apply { mkdir() }
        listOf("parts/a", "parts/b").forEach { part ->
            File(score, "$part/pages").mkdirs()
            ImageIO.write(sheet(100, 140, 10, 10, 90, 130), "png", File(score, "$part/pages/001.png"))
        }
        File(score, "parts/b/files").mkdirs()
        File(score, "parts/b/files/x.pdf").writeText("fingering")
        File(score, CitScore.MANIFEST_FILE).writeText(
            """{"metadata":{"title":"Duett"},"createdAt":"2025-01-01T00:00:00Z","parts":[
               {"directory":"parts/a","name":"A","content":{"type":"images","pages":["pages/001.png"]}},
               {"directory":"parts/b","name":"B","content":{"type":"images","pages":["pages/001.png"]},"files":[{"path":"files/x.pdf","name":"Fingersatz.pdf"}]}]}""",
        )
        val file = PlatformFile(score)
        assertEquals(listOf(Attachment("parts/b/files/x.pdf", "Fingersatz.pdf")), repository.attachments(file, AttachmentKind.File))

        repository.updateScore(PlatformFile(root), file, ScoreMetadata("Duett"), listOf(PartEdit.Keep("parts/a", "A")))

        assertEquals(listOf(Attachment("files/x.pdf", "Fingersatz.pdf")), repository.attachments(file, AttachmentKind.File))
        assertEquals("fingering", File(score, "files/x.pdf").readText())
        assertFalse(File(score, "parts/b").exists())
    }

    @Test
    fun `musicxml parts keep the original file and are engraved for the thumbnail`() = runTest {
        FileKit.init(appId = "citrinscore-test", filesDir = File(root, ".files"))
        val musicXml = File(root, "Walzer.MXL").apply { writeBytes(MusicXmlEngraverTest.mxl(MusicXmlEngraverTest.score(measures = 2))) }

        repository.importScore(PlatformFile(root), ScoreMetadata("Walzer"), listOf(NewPart("Klavier", PartSource.MusicXml(PlatformFile(musicXml)))))

        val score = File(root, "Walzer.citscore")
        val part = repository.load(PlatformFile(score)).parts.single()
        assertEquals(ScoreContent.MusicXml("score.mxl"), part.content)
        assertNull(part.systemBreaks)
        assertContentEquals(musicXml.readBytes(), File(score, "${part.directory}/score.mxl").readBytes())
        assertNotNull(repository.thumbnail(PlatformFile(score)))
    }

    private suspend fun importImages(metadata: ScoreMetadata, vararg images: File) {
        repository.importScore(PlatformFile(root), metadata, listOf(imagePart("", *images)))
    }

    private suspend fun imagePart(name: String, vararg images: File): NewPart {
        val files = images.map(::PlatformFile)
        return NewPart(name, PartSource.Images(files, files.map { file -> openImageSource(file).use { it.contentCrop(0) } }))
    }

    private suspend fun importPdf(title: String, pdf: File, crops: List<PageCrop>) {
        repository.importScore(PlatformFile(root), ScoreMetadata(title), listOf(NewPart("", PartSource.Pdf(PlatformFile(pdf), crops))))
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
            parts = listOf(ScorePart("", "", ScoreContent.Images(emptyList()))),
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
