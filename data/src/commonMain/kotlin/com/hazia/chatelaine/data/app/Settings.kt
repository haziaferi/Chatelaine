package com.hazia.chatelaine.data.app

import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

/**
 * The directory, under the app's files, that holds this device's own settings (`local/settings.json`): the folders
 * picked on this device and the time of its last backup mean nothing elsewhere, so Android's backup rules exclude it.
 */
const val LOCAL_DIR = "local"

/** This device's settings: not synced and not in the database. Folders are as the platform names them (a path, a tree URI). */
@Serializable
data class Settings(
    val syncFolder: String? = null,
    val backupFolder: String? = null,
    val language: String? = null,
    val lastBackupMillis: Long? = null,
    val dismissedNotices: Set<String> = emptySet(),
)

interface SettingsStore {
    val settings: StateFlow<Settings>

    /** Applies [change] to the settings, stores the result and returns it. */
    suspend fun update(change: (Settings) -> Settings): Settings
}
