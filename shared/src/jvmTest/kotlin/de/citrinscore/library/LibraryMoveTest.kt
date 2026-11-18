package de.citrinscore.library

import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LibraryMoveTest {

    private val root = Files.createTempDirectory("citrinscore-move").toFile()
    private val source = File(root, "source").apply {
        resolve("Etude.citscore/pages").mkdirs()
        resolve("Etude.citscore/manifest.json").writeText("{}")
        resolve("Bach").mkdirs()
        resolve(".git/objects").mkdirs()
    }
    private val target = File(root, "target").apply { mkdirs() }

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `scores and folders move to the target and discarded entries are removed`() = runTest {
        var committed = false

        val result = moveLibrary(PlatformFile(source), PlatformFile(target), setOf(".git")) { committed = true }

        assertEquals(LibraryMoveResult.Moved, result)
        assertTrue(committed)
        assertEquals("{}", target.resolve("Etude.citscore/manifest.json").readText())
        assertTrue(target.resolve("Bach").isDirectory)
        assertFalse(target.resolve(".git").exists())
        assertEquals(emptyList(), source.list()?.toList())
    }

    @Test
    fun `nothing moves when the target has an entry with the same name`() = runTest {
        target.resolve("Bach").mkdirs()

        val result = moveLibrary(PlatformFile(source), PlatformFile(target), emptySet()) { error("Must not commit") }

        assertEquals(LibraryMoveResult.TargetOccupied, result)
        assertTrue(source.resolve("Etude.citscore/manifest.json").exists())
        assertFalse(target.resolve("Etude.citscore").exists())
    }

    @Test
    fun `copies are removed and the source stays when committing fails`() = runTest {
        val result = moveLibrary(PlatformFile(source), PlatformFile(target), emptySet()) { throw IOException("disk full") }

        assertEquals(LibraryMoveResult.Failed, result)
        assertEquals(emptyList(), target.list()?.toList())
        assertTrue(source.resolve("Etude.citscore/manifest.json").exists())
    }
}
