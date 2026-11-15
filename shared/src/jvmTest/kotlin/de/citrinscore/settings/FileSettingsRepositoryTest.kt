package de.citrinscore.settings

import de.citrinscore.i18n.AppLanguage
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.writeString
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FileSettingsRepositoryTest {

    private val root = Files.createTempDirectory("citrinscore-test").toFile()
    private val directory = PlatformFile(root.resolve("config"))
    private val repository = FileSettingsRepository(directory)

    @AfterTest
    fun cleanUp() {
        root.deleteRecursively()
    }

    @Test
    fun `load returns null when no settings exist`() = runTest {
        assertNull(repository.load())
    }

    @Test
    fun `saved settings are loaded again`() = runTest {
        val library = PlatformFile(root.resolve("library").apply { mkdirs() })
        val settings = AppSettings(AppLanguage.German, LibraryReference.of(library))

        repository.save(settings)

        assertEquals(settings, repository.load())
    }

    @Test
    fun `corrupt settings are treated as missing`() = runTest {
        root.resolve("config").mkdirs()
        (directory / "settings.json").writeString("{ not json")

        assertNull(repository.load())
    }
}
