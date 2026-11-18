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
import kotlin.test.assertFalse
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
    fun `the access token is stored encrypted and loaded again`() = runTest {
        val settings = AppSettings(AppLanguage.English, LibraryLocation.App, git = GitRemote("https://example.org/library.git", "secret-token"))

        repository.save(settings)

        assertFalse("secret-token" in root.resolve("config/settings.json").readText())
        assertEquals(settings, FileSettingsRepository(directory).load())
    }

    @Test
    fun `a plain access token from older settings is encrypted when loaded`() = runTest {
        root.resolve("config").mkdirs()
        (directory / "settings.json").writeString(
            """{"language":"English","library":{},"git":{"url":"https://example.org/library.git","token":"secret-token"}}""",
        )

        assertEquals("secret-token", repository.load()?.git?.token)
        assertFalse("secret-token" in root.resolve("config/settings.json").readText())
    }

    @Test
    fun `settings stay usable when the token cannot be decrypted and only the remote is dropped`() = runTest {
        root.resolve("config").mkdirs()
        (directory / "settings.json").writeString(
            """{"language":"English","library":{},"git":{"url":"https://example.org/library.git","token":"aes-gcm:broken"}}""",
        )

        val settings = repository.load()

        assertEquals(LibraryLocation.App, settings?.library)
        assertNull(settings?.git)
    }

    @Test
    fun `corrupt settings are treated as missing`() = runTest {
        root.resolve("config").mkdirs()
        (directory / "settings.json").writeString("{ not json")

        assertNull(repository.load())
    }
}
