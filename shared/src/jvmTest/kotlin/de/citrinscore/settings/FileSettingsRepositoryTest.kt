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
        val settings = AppSettings(AppLanguage.German, LibraryLocation.Folder.of(library), ThemeMode.Dark)

        repository.save(settings)

        assertEquals(settings, repository.load())
    }

    @Test
    fun `a library in the app storage is loaded again`() = runTest {
        val settings = AppSettings(AppLanguage.English, LibraryLocation.App)

        repository.save(settings)

        assertEquals(settings, repository.load())
    }

    @Test
    fun `settings from before library locations keep their folder and follow the system theme`() = runTest {
        root.resolve("config").mkdirs()
        (directory / "settings.json").writeString("""{"language":"English","library":{"displayPath":"/music","bookmark":""}}""")

        val settings = repository.load()

        assertEquals(LibraryLocation.Folder("/music", ""), settings?.library)
        assertEquals(ThemeMode.System, settings?.theme)
    }

    @Test
    fun `corrupt settings are treated as missing`() = runTest {
        root.resolve("config").mkdirs()
        (directory / "settings.json").writeString("{ not json")

        assertNull(repository.load())
    }
}
