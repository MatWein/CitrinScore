package de.citrinscore.library

import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.serialization.AppJson
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import java.util.zip.ZipFile
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals

class LibraryRepositoryTest {

    private val root = Files.createTempDirectory("citrinscore-library").toFile()
    private val repository = LibraryRepository()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `content lists folders before scores and ignores other files`() = runTest {
        File(root, "Zeta").mkdir()
        File(root, "alpha").mkdir()
        File(root, ".hidden").mkdir()
        File(root, "Beta.citscore").writeText("")
        File(root, "notes.txt").writeText("")

        val names = repository.content(PlatformFile(root)).items.map { it.displayName }

        assertEquals(listOf("alpha", "Zeta", "Beta"), names)
    }

    @Test
    fun `created folders appear in the content`() = runTest {
        repository.createFolder(PlatformFile(root), "Chor")

        assertEquals(listOf("Chor"), repository.content(PlatformFile(root)).items.map { it.displayName })
    }

    @Test
    fun `imported images are stored in order with a manifest`() = runTest {
        val second = File(root, "b.JPG").apply { writeBytes(byteArrayOf(2)) }
        val first = File(root, "a.png").apply { writeBytes(byteArrayOf(1)) }
        val metadata = ScoreMetadata(title = "Etude", composer = "Chopin")

        repository.importImages(PlatformFile(root), metadata, listOf(PlatformFile(second), PlatformFile(first)))

        ZipFile(File(root, "Etude.citscore")).use { zip ->
            val manifest = AppJson.decodeFromString<ScoreManifest>(zip.read("manifest.json").decodeToString())
            assertEquals(metadata, manifest.metadata)
            assertEquals(ScoreContent.Images(listOf("pages/001.jpg", "pages/002.png")), manifest.content)
            assertContentEquals(byteArrayOf(2), zip.read("pages/001.jpg"))
            assertContentEquals(byteArrayOf(1), zip.read("pages/002.png"))
        }
    }

    private fun ZipFile.read(name: String) = getInputStream(getEntry(name)).use { it.readBytes() }
}
