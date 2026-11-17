package de.citrinscore.sync

import de.citrinscore.score.CitScore
import de.citrinscore.settings.GitRemote
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.test.runTest
import org.eclipse.jgit.api.Git
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GitLibrarySyncTest {

    private val root = Files.createTempDirectory("citrinscore-sync").toFile()
    private val origin = File(root, "origin.git").also { Git.init().setBare(true).setDirectory(it).setInitialBranch("main").call().close() }
    private val remote = GitRemote(url = origin.toURI().toString(), token = "token")
    private val first = File(root, "first").apply { mkdirs() }
    private val second = File(root, "second").apply { mkdirs() }

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `connecting validates the remote and prepares the library`() = runTest {
        GitLibrarySync.connect(PlatformFile(first), remote)

        assertTrue(File(first, ".git").isDirectory)
        assertTrue(File(first, ".gitignore").readText().contains("device.json"))
        assertFailsWith<SyncException> { GitLibrarySync.connect(PlatformFile(second), remote.copy(url = File(root, "missing").toURI().toString())) }
    }

    @Test
    fun `scores travel between libraries without device state`() = runTest {
        score(first, "Etude", "one")
        File(first, "Leer").mkdir()

        assertEquals(SyncOutcome.Completed(uploaded = 1, downloaded = 0), sync(first))
        assertEquals(SyncOutcome.Completed(uploaded = 0, downloaded = 1), sync(second))

        assertEquals("one", manifest(second, "Etude"))
        assertFalse(File(second, "Etude.citscore/${CitScore.DEVICE_STATE_FILE}").exists())
        assertTrue(File(second, "Leer").isDirectory)
    }

    @Test
    fun `deleted scores disappear including their device state`() = runTest {
        score(first, "Etude", "one")
        sync(first)
        sync(second)
        File(second, "Etude.citscore/${CitScore.DEVICE_STATE_FILE}").writeText("{}")

        File(first, "Etude.citscore").deleteRecursively()
        sync(first)

        assertEquals(SyncOutcome.Completed(uploaded = 0, downloaded = 1), sync(second))
        assertFalse(File(second, "Etude.citscore").exists())
    }

    @Test
    fun `both libraries keep their own scores on the first sync`() = runTest {
        score(first, "Etude", "one")
        score(second, "Sonate", "two")
        sync(first)

        assertEquals(SyncOutcome.Completed(uploaded = 1, downloaded = 1), sync(second))
        assertEquals(SyncOutcome.Completed(uploaded = 0, downloaded = 1), sync(first))
        assertEquals("two", manifest(first, "Sonate"))
        assertEquals("one", manifest(second, "Etude"))
    }

    @Test
    fun `conflicting scores keep the remote version when chosen`() = runTest {
        val conflicts = prepareConflict()

        val outcome = sync(second) { found -> conflicts += found; mapOf("Noten/Etude.citscore" to ConflictChoice.Remote) }

        assertEquals(listOf(SyncConflict("Noten/Etude.citscore", "Noten", "Etude")), conflicts)
        assertEquals(SyncOutcome.Completed(uploaded = 0, downloaded = 1), outcome)
        assertEquals("first", manifest(second, "Noten/Etude"))
        assertFalse(File(second, "Noten/Etude.citscore/extra.txt").exists())
    }

    @Test
    fun `conflicting scores keep the local version when chosen and upload it`() = runTest {
        prepareConflict()

        assertEquals(SyncOutcome.Completed(uploaded = 1, downloaded = 0), sync(second) { mapOf("Noten/Etude.citscore" to ConflictChoice.Local) })
        sync(first)

        assertEquals("second", manifest(first, "Noten/Etude"))
        assertTrue(File(first, "Noten/Etude.citscore/extra.txt").exists())
    }

    @Test
    fun `cancelling a conflict leaves the library untouched`() = runTest {
        prepareConflict()

        assertEquals(SyncOutcome.Cancelled, sync(second) { null })

        assertEquals("second", manifest(second, "Noten/Etude"))
        Git.open(second).use { git -> assertTrue(git.status().call().isClean) }
        assertEquals(SyncOutcome.Completed(uploaded = 1, downloaded = 0), sync(second) { mapOf("Noten/Etude.citscore" to ConflictChoice.Local) })
    }

    private suspend fun prepareConflict(): MutableList<SyncConflict> {
        score(first, "Noten/Etude", "base")
        sync(first)
        sync(second)
        score(first, "Noten/Etude", "first")
        sync(first)
        score(second, "Noten/Etude", "second")
        File(second, "Noten/Etude.citscore/extra.txt").writeText("local only")
        return mutableListOf()
    }

    private suspend fun sync(library: File, resolve: suspend (List<SyncConflict>) -> Map<String, ConflictChoice>? = { error("Unexpected conflict $it") }) =
        GitLibrarySync.synchronize(PlatformFile(library), remote, progress = {}, resolve = resolve)

    private fun score(library: File, path: String, title: String) {
        val directory = File(library, "$path.citscore").apply { mkdirs() }
        File(directory, CitScore.MANIFEST_FILE).writeText(title)
        File(directory, CitScore.DEVICE_STATE_FILE).writeText("""{"lastOpenedAt":"2026-01-01T00:00:00Z"}""")
    }

    private fun manifest(library: File, path: String) = File(library, "$path.citscore/${CitScore.MANIFEST_FILE}").readText()
}
