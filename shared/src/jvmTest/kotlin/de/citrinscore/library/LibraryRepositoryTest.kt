package de.citrinscore.library

import de.citrinscore.score.CitScore
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.serialization.AppJson
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Files
import javax.imageio.ImageIO
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.math.abs
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

        val names = repository.content(PlatformFile(root)).items.map { it.displayName }

        assertEquals(listOf("alpha", "Zeta", "Reopened", "New", "Old", "Broken"), names)
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

        repository.importImages(PlatformFile(root), metadata, listOf(PlatformFile(second), PlatformFile(first)))

        val score = File(root, "Etude.citscore")
        val manifest = AppJson.decodeFromString<ScoreManifest>(File(score, CitScore.MANIFEST_FILE).readText())
        assertEquals(metadata, manifest.metadata)
        assertEquals(now, manifest.createdAt)
        assertNull(manifest.lastOpenedAt)
        assertEquals(ScoreContent.Images(listOf("pages/001.jpg", "pages/002.png")), manifest.content)
        assertContentEquals(second.readBytes(), File(score, "pages/001.jpg").readBytes())
        assertContentEquals(first.readBytes(), File(score, "pages/002.png").readBytes())
    }

    @Test
    fun `thumbnail is a page sized excerpt of all stacked pages`() = runTest {
        val firstLine = image("1.png", "png", width = 1200, height = 600, color = Color.RED)
        val secondLine = image("2.png", "png", width = 2400, height = 1200, color = Color.BLUE)
        repository.importImages(PlatformFile(root), ScoreMetadata(title = "Etude"), listOf(PlatformFile(firstLine), PlatformFile(secondLine)))

        val thumbnail = assertNotNull(repository.thumbnail(PlatformFile(File(root, "Etude.citscore"))))

        val decoded = ImageIO.read(thumbnail.inputStream())
        assertEquals(CitScore.THUMBNAIL_WIDTH, decoded.width)
        assertEquals(849, decoded.height)
        assertColorNear(Color.RED, Color(decoded.getRGB(300, 150)))
        assertColorNear(Color.BLUE, Color(decoded.getRGB(300, 450)))
        assertColorNear(Color.WHITE, Color(decoded.getRGB(300, 750)))
    }

    @Test
    fun `thumbnail is missing for incomplete scores`() = runTest {
        val incomplete = File(root, "Incomplete.citscore").apply { mkdir() }

        assertNull(repository.thumbnail(PlatformFile(incomplete)))
    }

    @Test
    fun `failed imports leave no partial score behind`() = runTest {
        val notAnImage = File(root, "broken.png").apply { writeText("not an image") }

        runCatching { repository.importImages(PlatformFile(root), ScoreMetadata(title = "Etude"), listOf(PlatformFile(notAnImage))) }

        assertFalse(File(root, "Etude.citscore").exists())
    }

    private suspend fun importAt(title: String, time: Instant) {
        now = time
        val page = image("$title.png", "png", width = 10, height = 14)
        repository.importImages(PlatformFile(root), ScoreMetadata(title), listOf(PlatformFile(page)))
        page.delete()
    }

    private fun score(title: String, createdAt: String, lastOpenedAt: String) {
        val manifest = ScoreManifest(
            metadata = ScoreMetadata(title),
            content = ScoreContent.Images(emptyList()),
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

    private fun assertColorNear(expected: Color, actual: Color) {
        val distance = maxOf(abs(expected.red - actual.red), abs(expected.green - actual.green), abs(expected.blue - actual.blue))
        assertTrue(distance <= 16, "expected $expected but was $actual")
    }
}
