package com.hazia.chatelaine.data

import com.hazia.chatelaine.data.backup.Backup
import com.hazia.chatelaine.data.backup.ZipBackup
import com.hazia.chatelaine.data.blob.BlobSync
import com.hazia.chatelaine.data.blob.BlobStore
import com.hazia.chatelaine.data.sync.FolderSync
import com.hazia.chatelaine.data.sync.ImportLock
import com.hazia.chatelaine.data.sync.PlainLines
import com.hazia.chatelaine.data.sync.SyncFolder
import com.hazia.chatelaine.data.sync.loadClock
import kotlinx.coroutines.runBlocking

/** An item as the debug harnesses list it: its names that a later edit overwrote ([lost]) are what History holds. */
data class HarnessItem(val id: String, val name: String, val lost: List<String>)

/**
 * The wiring of one device's data layer (database, clock, folder sync, photo files, backup) behind the five actions
 * of the debug harnesses of stage 1. PROVISIONAL: it exists so the harnesses, which live in other modules, can reach
 * the internal `FolderSync` and `BlobSync`; stage 2 replaces it with the app's real composition.
 */
class ChatelaineData(
    open: DatabaseOpen<ChatelaineDatabase>,
    val deviceId: String,
    private val photos: BlobStore,
    private val wallMillis: () -> Long = System::currentTimeMillis,
) : AutoCloseable {
    private val db = open.database
    // Resumed from the database (blocking, as opening it is): a fresh clock would stamp the next edit below a version
    // already seen from a peer whose clock runs ahead, and that edit would lose everywhere but here.
    private val clock = runBlocking { db.syncDao().loadClock(deviceId, wallMillis) }
    private val writes = LocalWrites(db, clock)
    private val inventory = Inventory(db, writes)
    private val history = History(db, writes)
    private val lock = ImportLock()
    private var current: Pair<SyncFolder, FolderSync>? = null
    private val backup = Backup(
        db, photos, clock, ZipBackup, wallMillis, importLock = lock,
    )

    suspend fun add(name: String): String = inventory.addItem(name)

    suspend fun edit(id: String, name: String) = inventory.rename(id, name)

    suspend fun list(): List<HarnessItem> =
        inventory.items().sortedBy { it.name }.map { item ->
            HarnessItem(item.id, item.name, history.overwritten(ITEM, item.id, NAME).mapNotNull { it.values["name"] as? String })
        }

    /** Exports, imports and exchanges photo files with [folder], until an import changes nothing; returns the rounds. */
    suspend fun sync(folder: SyncFolder): Int {
        // One FolderSync per folder; what it read, and whether the own log is still to be read back, is in the database.
        val s = current?.takeIf { it.first === folder }?.second ?: FolderSync(
            db, folder, deviceId, clock, db.syncedTables(), PlainLines, wallMillis, importLock = lock,
        ).also { current = folder to it }
        var rounds = 0
        do {
            rounds++
            s.export()
            val changed = s.import().changed
            BlobSync(db, folder, photos, wallMillis).exchange()
        } while (changed > 0 && rounds < MAX_ROUNDS)
        return rounds
    }

    /**
     * Today's backup in [folder], the last seven kept; its name, or null while a database made anew after a corrupt one
     * has not read its own log back.
     */
    suspend fun backup(folder: SyncFolder): String? = backup.daily(folder, wallMillis())

    override fun close() = db.close()

    private companion object {
        const val MAX_ROUNDS = 10
    }
}
