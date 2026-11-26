package com.hazia.chatelaine.data.app

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING

/**
 * The settings as JSON in [file], `File(filesDir, "$LOCAL_DIR/settings.json")` on both platforms. Read once, when
 * made (blocking); each update is written to a temporary file and moved into place, so a crash leaves the old
 * settings or the new ones. A file that does not read as settings is renamed with the suffix `.unreadable` (one kept
 * before is replaced) and the defaults are used.
 */
class FileSettingsStore(private val file: File) : SettingsStore {
    private val lock = Mutex()
    private val state = MutableStateFlow(load())
    override val settings: StateFlow<Settings> = state.asStateFlow()

    override suspend fun update(change: (Settings) -> Settings): Settings = lock.withLock {
        val next = change(state.value)
        withContext(Dispatchers.IO) { write(next) }
        state.value = next
        next
    }

    private fun load(): Settings {
        if (!file.exists()) return Settings()
        return runCatching { JSON.decodeFromString(Settings.serializer(), file.readText()) }.getOrElse {
            Files.move(file.toPath(), File(file.path + UNREADABLE_SUFFIX).toPath(), REPLACE_EXISTING)
            Settings()
        }
    }

    private fun write(settings: Settings) {
        val dir = requireNotNull(file.absoluteFile.parentFile) { "no directory for ${file.path}" }.apply { mkdirs() }
        val tmp = Files.createTempFile(dir.toPath(), file.name, ".tmp")
        Files.write(tmp, JSON.encodeToString(Settings.serializer(), settings).encodeToByteArray())
        Files.move(tmp, file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
    }

    private companion object {
        const val UNREADABLE_SUFFIX = ".unreadable"

        // A setting only a later version knows is ignored, not a reason to set the whole file aside.
        val JSON = Json { ignoreUnknownKeys = true }
    }
}
