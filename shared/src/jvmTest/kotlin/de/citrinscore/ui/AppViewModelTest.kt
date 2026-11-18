package de.citrinscore.ui

import de.citrinscore.i18n.AppLanguage
import de.citrinscore.settings.AppSettings
import de.citrinscore.settings.LibraryLocation
import de.citrinscore.settings.SettingsRepository
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {

    private val root = Files.createTempDirectory("citrinscore-app").toFile()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun cleanUp() {
        Dispatchers.resetMain()
        root.deleteRecursively()
    }

    @Test
    fun `without settings the onboarding starts`() = runTest {
        assertEquals(AppState.Onboarding, AppViewModel(FakeSettings(null)).state.value)
    }

    @Test
    fun `a missing library folder is reported instead of restarting the onboarding`() = runTest {
        val library = File(root, "Noten").apply { mkdir() }
        val settings = AppSettings(AppLanguage.English, LibraryLocation.Folder.of(PlatformFile(library)))
        library.delete()

        val viewModel = AppViewModel(FakeSettings(settings))

        assertEquals(AppState.LibraryUnavailable(settings), viewModel.state.value)

        var failed = false
        viewModel.retryLibrary { failed = true }
        assertTrue(failed)

        library.mkdir()
        viewModel.retryLibrary { error("Library should be available again") }
        assertIs<AppState.Ready>(viewModel.state.value)
    }

    private class FakeSettings(private var settings: AppSettings?) : SettingsRepository {
        override suspend fun load() = settings

        override suspend fun save(settings: AppSettings) {
            this.settings = settings
        }
    }
}
