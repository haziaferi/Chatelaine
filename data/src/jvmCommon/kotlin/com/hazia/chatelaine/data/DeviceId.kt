package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.Ids
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * The directory, under the app's files, that holds the device id file (`identity/device-id`) and the record of its
 * last renewal (`identity/renewal`), nothing else: Android's backup rules exclude this whole directory (R59), so a
 * restored or transferred phone makes its own id.
 */
const val DEVICE_ID_DIR = "identity"

/**
 * This device's sync identity, kept in its own [file]: not a setting and not in the database, so a settings reset
 * leaves it as it is. A database made anew after a corrupt one takes a new one through [renewDeviceIdIfRecovered],
 * which the app calls on every start; this function alone never renews it. A UUID, as every Chatelaine id is (R52).
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

/**
 * The device id to use, renewed when a database was made anew after a corrupt one (Decision 8): a database made anew
 * that kept the old id would start a second log under it, which clashes with the old log in the folder. [keptFiles]
 * are the names of the unopenable files kept beside the database (`keptFilesBeside`); with none outside the last
 * renewal's list, the id in `identity/device-id` ([deviceId]) is returned as it is.
 *
 * Otherwise it renews, in steps each of which a crash may cut short:
 * 1. writes `identity/renewal`: the current id as `old`, every kept file, and `marked` false;
 * 2. writes a new id into `identity/device-id`;
 * 3. calls [markRecovered] with the old id, so the database reads that id's log back as a peer's
 *    (`OwnLogEntity.previousDevice`) and holds backups until it has;
 * 4. rewrites `renewal` with `marked` true.
 *
 * Both files are written to a temporary file and moved into place. A start that finds `renewal` not marked finishes
 * it first: it writes the new id if `device-id` still holds `renewal.old`, then calls [markRecovered] again, which
 * puts the same row. An unreadable `renewal` counts as none. [now] stamps an unreadable id file set aside.
 */
suspend fun renewDeviceIdIfRecovered(
    identityDir: File,
    keptFiles: List<String>,
    markRecovered: suspend (previous: String) -> Unit,
    now: () -> Long = System::currentTimeMillis,
): String {
    val idFile = File(identityDir, DEVICE_ID_FILE)
    val renewalFile = File(identityDir, RENEWAL_FILE)
    var id = deviceId(idFile, now)
    val last = readRenewal(renewalFile)
    if (last != null && !last.marked) {
        if (id == last.old) id = writeNewId(idFile)
        markRecovered(last.old)
        replaceText(renewalFile, RENEWAL_JSON.encodeToString(Renewal.serializer(), last.copy(marked = true)))
    }
    if (keptFiles.all { it in last?.kept.orEmpty() }) return id

    val renewal = Renewal(old = id, kept = keptFiles, marked = false)
    replaceText(renewalFile, RENEWAL_JSON.encodeToString(Renewal.serializer(), renewal))
    val renewed = writeNewId(idFile)
    markRecovered(renewal.old)
    replaceText(renewalFile, RENEWAL_JSON.encodeToString(Renewal.serializer(), renewal.copy(marked = true)))
    return renewed
}

/** The last renewal of the device id: the id it replaced, the kept files it was made for, and whether the database was marked. */
@Serializable
private data class Renewal(val old: String, val kept: List<String>, val marked: Boolean)

private const val DEVICE_ID_FILE = "device-id"
private const val RENEWAL_FILE = "renewal"
private val RENEWAL_JSON = Json { ignoreUnknownKeys = true }

private fun readRenewal(file: File): Renewal? {
    if (!file.exists()) return null
    return runCatching { RENEWAL_JSON.decodeFromString(Renewal.serializer(), file.readText()) }.getOrNull()
}

private fun writeNewId(file: File): String = Ids.new().also { replaceText(file, it) }

/** Writes [text] to a temporary file beside [file] and moves it into place, so a crash leaves the old file or the new one. */
private fun replaceText(file: File, text: String) {
    val dir = requireNotNull(file.absoluteFile.parentFile) { "no directory for ${file.path}" }.apply { mkdirs() }
    val tmp = Files.createTempFile(dir.toPath(), file.name, ".tmp")
    Files.write(tmp, text.encodeToByteArray())
    Files.move(tmp, file.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
}
