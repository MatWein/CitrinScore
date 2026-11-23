package de.citrinscore.library

import de.citrinscore.score.CitScore
import de.citrinscore.score.ScoreContent
import de.citrinscore.score.ScoreManifest
import de.citrinscore.score.ScoreMetadata
import de.citrinscore.score.ScorePart
import de.citrinscore.serialization.AppJson
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class LibraryTransferTest {

    private val root = Files.createTempDirectory("citrinscore-transfer").toFile()
    private val repository = LibraryRepository()

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `free names stay and taken names get the next number`() {
        assertEquals("Chor", availableName("Chor", listOf("Orchester")))
        assertEquals("Chor (2)", availableName("Chor", listOf("chor")))
        assertEquals("Chor (3)", availableName("Chor", listOf("Chor", "Chor (2)")))
        assertEquals("Chor (3)", availableName("Chor (2)", listOf("Chor (2)")))
        assertEquals("Etude (2).citscore", availableName("Etude.citscore", listOf("Etude.citscore")))
    }

    @Test
    fun `moved items leave the source and keep their content`() = runTest {
        val target = File(root, "Ziel").apply { mkdir() }
        File(root, "Chor").apply { mkdir() }.resolve("Lied.citscore").mkdir()
        score(root, "Etude")

        val failed = repository.transfer(listOf(file("Chor"), file("Etude.citscore")), PlatformFile(root), PlatformFile(target), TransferMode.Move)

        assertEquals(emptyList(), failed)
        assertEquals(setOf("Ziel"), root.list()!!.toSet())
        assertEquals(setOf("Chor", "Etude.citscore"), target.list()!!.toSet())
        assertTrue(File(target, "Chor/Lied.citscore").isDirectory)
    }

    @Test
    fun `copies keep the source and taken names are numbered with the score title`() = runTest {
        val target = File(root, "Ziel").apply { mkdir() }
        score(root, "Etude")
        score(target, "Etude")

        repository.transfer(listOf(file("Etude.citscore")), PlatformFile(root), PlatformFile(target), TransferMode.Copy)

        assertTrue(File(root, "Etude.citscore").isDirectory)
        assertEquals(setOf("Etude.citscore", "Etude (2).citscore"), target.list()!!.toSet())
        assertEquals("Etude (2)", repository.load(PlatformFile(File(target, "Etude (2).citscore"))).metadata.title)
        assertEquals("Etude", repository.load(PlatformFile(File(root, "Etude.citscore"))).metadata.title)
    }

    @Test
    fun `a folder is not moved into itself`() = runTest {
        val folder = File(root, "Chor").apply { mkdir() }

        val failed = repository.transfer(listOf(file("Chor")), PlatformFile(root), PlatformFile(folder), TransferMode.Move)

        assertEquals(listOf("Chor"), failed.map { File(it.toString()).name })
        assertTrue(folder.isDirectory)
        assertFalse(File(folder, "Chor").exists())
    }

    private fun file(name: String) = PlatformFile(File(root, name))

    private fun score(folder: File, title: String) {
        val manifest = ScoreManifest(
            metadata = ScoreMetadata(title),
            parts = listOf(ScorePart("parts/a", "", ScoreContent.Pages(emptyList()))),
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
        File(folder, CitScore.directoryName(title)).apply { mkdir() }.resolve(CitScore.MANIFEST_FILE).writeText(AppJson.encodeToString(manifest))
    }
}
