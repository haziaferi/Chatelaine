package com.hazia.chatelaine.data.app

import com.hazia.chatelaine.data.Fields
import com.hazia.chatelaine.data.History
import com.hazia.chatelaine.data.Inventory
import com.hazia.chatelaine.data.sync.ImportReport
import com.hazia.chatelaine.data.sync.SyncFolder
import kotlinx.coroutines.flow.Flow

/**
 * One device's data as the app uses it: the database behind [inventory], [fields] and [history], its folder sync,
 * photo files and backups. One per process (`LocalAppData.shared`).
 */
interface AppData {
    /** This device's sync identity; a new one after a recovery (`renewDeviceIdIfRecovered`). */
    val deviceId: String
    val inventory: Inventory
    val fields: Fields
    val history: History

    /** Emits once at start, then after each commit that touches one of [tables] (by their table names). */
    fun changes(vararg tables: String): Flow<Unit>

    /**
     * Exports, imports and exchanges photo files with [folder], until an import changes nothing, at most ten rounds;
     * returns the rounds.
     */
    suspend fun sync(folder: SyncFolder): Int

    /** Rows and purges waiting to be exported: what the next sync would write. */
    suspend fun pendingExport(): Int

    /**
     * Today's backup in [folder], the last seven kept; its name, or null while a database made anew after a corrupt one
     * has neither read its own log back nor had a backup restored.
     */
    suspend fun backup(folder: SyncFolder, now: Long): String?

    /** Merges the backup in [zip] as a peer's changes (R61). */
    suspend fun restore(zip: ByteArray): ImportReport

    suspend fun recovery(): RecoveryState
}

/**
 * Where a recovery stands. [keptFiles] are the unopenable database files kept beside the database (R50), whose
 * presence alone makes the recovery notice [due], also after a crash that left the database unmarked;
 * [waitingForOwnLog] while the database made anew has not read its old log back; [backupsHeld] while backups wait for
 * that log or for a restored backup.
 */
data class RecoveryState(val keptFiles: List<String>, val waitingForOwnLog: Boolean, val backupsHeld: Boolean) {
    val due: Boolean get() = keptFiles.isNotEmpty()
}
