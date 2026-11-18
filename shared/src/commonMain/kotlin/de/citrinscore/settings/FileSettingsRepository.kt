package de.citrinscore.settings

import de.citrinscore.serialization.AppJson
import de.citrinscore.util.attempt
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.exists
import io.github.vinceglb.filekit.readString
import io.github.vinceglb.filekit.writeString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(FileSettingsRepository::class.java)

class FileSettingsRepository(
    private val directory: PlatformFile,
    private val json: Json = AppJson,
) : SettingsRepository {

    private val file get() = directory / FILE_NAME

    override suspend fun load(): AppSettings? {
        if (!file.exists()) return null
        val stored = try {
            json.decodeFromString<AppSettings>(file.readString())
        } catch (e: IllegalArgumentException) {
            logger.warn("Settings {} are invalid", file, e)
            return null
        }
        val remote = stored.git ?: return stored
        val token = attempt { TokenCipher.decrypt(remote.token) }
        if (token == null) logger.warn("The stored token for {} could not be decrypted, removing the synchronization", remote)
        val settings = stored.copy(git = token?.let { remote.copy(token = it) })
        if (!TokenCipher.isEncrypted(remote.token)) attempt { save(settings) }
        return settings
    }

    override suspend fun save(settings: AppSettings) {
        directory.createDirectories()
        val stored = settings.git?.let { settings.copy(git = it.copy(token = TokenCipher.encrypt(it.token))) } ?: settings
        file.writeString(json.encodeToString(stored))
    }

    private companion object {
        const val FILE_NAME = "settings.json"
    }
}
