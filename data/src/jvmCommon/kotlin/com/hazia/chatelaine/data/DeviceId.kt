package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.Ids
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The directory, under the app's files, that holds the device id file (`identity/device-id`) and nothing else:
 * Android's backup rules exclude this whole directory (R59), so a restored or transferred phone makes its own id.
 */
const val DEVICE_ID_DIR = "identity"

/**
 * This device's sync identity, kept in its own [file]: not a setting and not in the database, so neither a
 * settings reset nor a recovered database gives the device a new one. A UUID, as every Chatelaine id is (R52).
 *
 * Created once: the new id is written to a temporary file and moved into place only if no other
 * process got there first, in which case that process's id is used. An unreadable file is set
 * aside and a new identity made; peers then see a new device, which costs only a stale name.
 */
@OptIn(ExperimentalUuidApi::class)
fun deviceId(file: File, now: () -> Long = System::currentTimeMillis): String {
    if (file.exists()) {
        val id = file.readText().trim()
        if (Uuid.parseOrNull(id) != null) return id
        setAsideDatabaseFiles(file.path, now())
    }
    val dir = requireNotNull(file.absoluteFile.parentFile) { "no directory for ${file.path}" }.apply { mkdirs() }
    val tmp = Files.createTempFile(dir.toPath(), file.name, ".tmp")
    // Files.write, not Files.writeString: Android before API 33 has no writeString.
    Files.write(tmp, Ids.new().encodeToByteArray())
    try {
        Files.move(tmp, file.toPath())
    } catch (_: FileAlreadyExistsException) {
        Files.delete(tmp)
    }
    return file.readText().trim()
}
