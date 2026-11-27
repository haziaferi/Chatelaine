package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.Ids
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
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
    writeThroughTemp(file, Ids.new().encodeToByteArray(), replace = false)
    return file.readText().trim()
}

/**
 * An unopenable database file kept beside the database (R50), known by its [name], [size] in bytes and [modified]
 * time in epoch milliseconds: a later corruption kept under a name a deleted one had is another file.
 */
@Serializable
data class KeptFile(val name: String, val size: Long, val modified: Long)

/**
 * The device id to use, renewed when a database was made anew after a corrupt one (Decision 8): a database made anew
 * that kept the old id would start a second log under it, which clashes with the old log in the folder. [keptFiles]
 * are the unopenable files kept beside the database (`keptFileRecordsBeside`); with none outside the last renewal's
 * list (same name, size and time), the id in `identity/device-id` ([deviceId]) is returned as it is.
 *
 * Otherwise, when [ownLogPending] (the database has not read its own log back: one made anew after a corrupt one, or
 * a new one a crash left before it was marked), it renews, in steps each of which a crash may cut short:
 * 1. writes `identity/renewal`: the current id as `old`, every kept file, and `marked` false;
 * 2. writes a new id into `identity/device-id`;
 * 3. calls [markRecovered] with the old id, so the database reads that id's log back as a peer's
 *    (`OwnLogEntity.previousDevice`) and holds backups until it has;
 * 4. rewrites `renewal` with `marked` true.
 *
 * Beside an established database (not [ownLogPending]: its log was read back long ago, the kept file is an old one)
 * it only notes the kept files, in a `renewal` with no `old` and `marked` true: the id stays, and nothing is held,
 * which with no sync folder would be for ever.
 *
 * Both files are written to a temporary file and moved into place. A start that finds a `renewal` with an `old` id
 * not marked finishes it first: it writes the new id if `device-id` still holds `renewal.old`, then calls
 * [markRecovered] again, which puts the same row. An unreadable `renewal` counts as none. [now] stamps an unreadable
 * id file set aside.
 */
suspend fun renewDeviceIdIfRecovered(
    identityDir: File,
    keptFiles: List<KeptFile>,
    ownLogPending: Boolean,
    markRecovered: suspend (previous: String) -> Unit,
    now: () -> Long = System::currentTimeMillis,
): String {
    val idFile = File(identityDir, DEVICE_ID_FILE)
    val renewalFile = File(identityDir, RENEWAL_FILE)
    var id = deviceId(idFile, now)
    val last = readRenewal(renewalFile)
    if (last?.old != null && !last.marked) {
        if (id == last.old) id = writeNewId(idFile)
        markRecovered(last.old)
        writeRenewal(renewalFile, last.copy(marked = true))
    }
    if (keptFiles.all { it in last?.kept.orEmpty() }) return id
    if (!ownLogPending) {
        writeRenewal(renewalFile, Renewal(old = null, kept = keptFiles, marked = true))
        return id
    }

    val renewal = Renewal(old = id, kept = keptFiles, marked = false)
    writeRenewal(renewalFile, renewal)
    val renewed = writeNewId(idFile)
    markRecovered(id)
    writeRenewal(renewalFile, renewal.copy(marked = true))
    return renewed
}

/**
 * The last renewal of the device id: the id it replaced ([old]; null when it only noted kept files beside an
 * established database), the kept files it was made for, and whether the database was marked.
 */
@Serializable
private data class Renewal(val old: String?, val kept: List<KeptFile>, val marked: Boolean)

private fun writeRenewal(file: File, renewal: Renewal) = replaceText(file, RENEWAL_JSON.encodeToString(Renewal.serializer(), renewal))

private const val DEVICE_ID_FILE = "device-id"
private const val RENEWAL_FILE = "renewal"
private val RENEWAL_JSON = Json { ignoreUnknownKeys = true }

private fun readRenewal(file: File): Renewal? {
    if (!file.exists()) return null
    return runCatching { RENEWAL_JSON.decodeFromString(Renewal.serializer(), file.readText()) }.getOrNull()
}

private fun writeNewId(file: File): String = Ids.new().also { replaceText(file, it) }

private fun replaceText(file: File, text: String) {
    writeThroughTemp(file, text.encodeToByteArray())
}
