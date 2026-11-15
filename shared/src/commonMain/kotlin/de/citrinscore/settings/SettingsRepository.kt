package de.citrinscore.settings

interface SettingsRepository {
    suspend fun load(): AppSettings?
    suspend fun save(settings: AppSettings)
}
