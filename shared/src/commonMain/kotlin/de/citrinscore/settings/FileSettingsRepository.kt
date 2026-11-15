package de.citrinscore.settings

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.serialization.json.Json

class FileSettingsRepository(
    private val directory: PlatformFile,
    private val json: Json = Json { prettyPrint = true; ignoreUnknownKeys = true },
) : SettingsRepository {

    private val file get() = directory / FILE_NAME

    override suspend fun load(): AppSettings? {
        if (!file.exists()) return null
        return try {
            json.decodeFromString<AppSettings>(file.readString())
        } catch (_: IllegalArgumentException) {
            null
        }
    }

    override suspend fun save(settings: AppSettings) {
        directory.createDirectories()
        file.writeString(json.encodeToString(settings))
    }

    private companion object {
        const val FILE_NAME = "settings.json"
    }
}
