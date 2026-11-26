package com.hazia.chatelaine.data.sync

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.hazia.chatelaine.core.sync.HybridClock
import com.hazia.chatelaine.core.sync.Stamp

/** The permanent purge registry: one row per purged id, never expired. Ids are unique across tables (UUIDs). */
@Entity(tableName = "purge_registry")
internal data class PurgeEntity(
    @PrimaryKey val id: String,
    val hlc: Long,
    val device: String,
)

/** The highest clock value this device has issued or seen; one row, local only. */
@Entity(tableName = "clock")
internal data class ClockEntity(
    @PrimaryKey val id: Int = 0,
    @ColumnInfo(name = "last_hlc") val lastHlc: Long,
)

/**
 * Present while this database has not read its own log back from the folder (R59); one row, local only, kept in the
 * database so it outlives the process. Every new database starts with it (`SchemaTriggers.onCreate`); it is set again
 * when an import or a restore takes back a purge. [recovered] marks a database made anew after a corrupt one was set
 * aside: its own log is surely in the folder, so it waits until that log is there and read. Without it, a device with
 * no log of its own in the folder (a new device) has nothing to read back, and the mark goes on its first import or
 * export. [backupsHeld] keeps backups off meanwhile, as the near-empty database would rotate out the good ones; set
 * with [recovered], it goes when the own log is read or a backup is restored ([backupRestored]), which is the way back
 * for a device with no sync folder.
 */
@Entity(tableName = "sync_own_log")
internal data class OwnLogEntity(
    @PrimaryKey val id: Int = 0,
    val recovered: Boolean,
    @ColumnInfo(name = "backups_held") val backupsHeld: Boolean = false,
)

/** The last stamp both sides agreed on for an asking group; local only. */
@Entity(tableName = "sync_base", primaryKeys = ["id", "grp"])
internal data class BaseEntity(
    val id: String,
    val grp: String,
    val hlc: Long,
    val device: String,
)

/** A clash waiting for a person: the other side's group, as a folder record encodes it; local only. */
@Entity(tableName = "sync_ask", primaryKeys = ["id", "grp"])
internal data class AskEntity(
    val id: String,
    val grp: String,
    val theirs: String,
)

/** How far into a peer's file the importer has read, in bytes; local only. */
@Entity(tableName = "sync_read")
internal data class ReadEntity(
    @PrimaryKey val path: String,
    @ColumnInfo(name = "read_bytes") val readBytes: Long,
)

/**
 * A row or purge to export, queued by triggers on every synced table and on the purge registry
 * ([com.hazia.chatelaine.data.SchemaTriggers]), and cleared once the export is in the folder. [table] is
 * null for a purge.
 */
@Entity(tableName = "sync_outbox")
internal data class OutboxEntity(
    @PrimaryKey(autoGenerate = true) val n: Long = 0,
    val id: String,
    @ColumnInfo(name = "tbl") val table: String?,
)

/**
 * The synced tables the last import knew, as sorted names; one row, local only. When they change,
 * the read positions are dropped, so lines skipped for a table this version lacked are read again.
 */
@Entity(tableName = "sync_tables")
internal data class KnownTablesEntity(
    @PrimaryKey val id: Int = 0,
    val names: String,
)

/**
 * A folder line whose parent has not arrived yet, such as a child row read before the
 * parent it names; local only. Retried after every import, applied once its parent is here.
 */
@Entity(tableName = "sync_waiting", indices = [Index("line", unique = true)])
internal data class WaitingEntity(
    @PrimaryKey(autoGenerate = true) val n: Long = 0,
    val line: String,
)

/**
 * A version the merge replaced in a keep-loser group (`SyncStore.lost`), local only,
 * waiting for the after-import step to keep it in the page's History and raise its notice.
 */
@Entity(tableName = "sync_lost", indices = [Index(value = ["tbl", "row_id", "grp"])])
internal data class LostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name = "row_id") val rowId: String,
    val tbl: String,
    val grp: String,
    /** The replaced group, as a record line encodes one (its stamp included). */
    @ColumnInfo(name = "group_json") val groupJson: String,
)

@Dao
internal interface SyncDao {
    @Insert
    suspend fun addLost(rows: List<LostEntity>)

    @Query("SELECT * FROM sync_lost ORDER BY id")
    suspend fun lost(): List<LostEntity>

    @Query("SELECT * FROM sync_lost WHERE tbl = :table AND row_id = :rowId AND grp = :group ORDER BY id")
    suspend fun lostFor(table: String, rowId: String, group: String): List<LostEntity>

    @Query("SELECT * FROM sync_lost WHERE id = :id")
    suspend fun lostById(id: Long): LostEntity?

    @Query("DELETE FROM sync_lost WHERE id IN (:ids)")
    suspend fun clearLost(ids: List<Long>)

    @Query("SELECT * FROM purge_registry")
    suspend fun purges(): List<PurgeEntity>

    @Query("SELECT * FROM purge_registry WHERE id IN (:ids)")
    suspend fun purges(ids: List<String>): List<PurgeEntity>

    @Upsert
    suspend fun putPurge(purge: PurgeEntity)

    @Query("DELETE FROM purge_registry WHERE id = :id")
    suspend fun removePurge(id: String)

    @Query("SELECT last_hlc FROM clock WHERE id = 0")
    suspend fun lastHlc(): Long?

    @Upsert
    suspend fun saveClock(clock: ClockEntity)

    @Query("SELECT * FROM sync_own_log WHERE id = 0")
    suspend fun ownLog(): OwnLogEntity?

    @Upsert
    suspend fun putOwnLog(state: OwnLogEntity)

    /** The own log was read back: the mark goes. */
    @Query("DELETE FROM sync_own_log")
    suspend fun ownLogRead()

    @Query("SELECT * FROM sync_base WHERE id IN (:ids)")
    suspend fun bases(ids: List<String>): List<BaseEntity>

    @Upsert
    suspend fun putBases(bases: List<BaseEntity>)

    @Query("SELECT * FROM sync_ask WHERE id IN (:ids)")
    suspend fun asks(ids: List<String>): List<AskEntity>

    @Upsert
    suspend fun putAsks(asks: List<AskEntity>)

    @Query("DELETE FROM sync_ask WHERE id = :id AND grp = :group")
    suspend fun removeAsk(id: String, group: String)

    @Query("SELECT * FROM sync_read")
    suspend fun reads(): List<ReadEntity>

    @Query("DELETE FROM sync_read")
    suspend fun clearReads()

    @Upsert
    suspend fun putRead(read: ReadEntity)

    /** Forgets positions in files no longer in the folder. */
    @Query("DELETE FROM sync_read WHERE path NOT IN (:present)")
    suspend fun keepReads(present: List<String>)

    /** A line already waiting (a peer's snapshot repeats it) is not added twice. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun wait(lines: List<WaitingEntity>)

    @Query("SELECT * FROM sync_waiting ORDER BY n")
    suspend fun waiting(): List<WaitingEntity>

    @Query("DELETE FROM sync_waiting WHERE n = :n")
    suspend fun stopWaiting(n: Long)

    @Query("SELECT id, grp FROM sync_ask ORDER BY id, grp")
    suspend fun allAsks(): List<AskKey>

    @Query("SELECT names FROM sync_tables WHERE id = 0")
    suspend fun knownTables(): String?

    @Upsert
    suspend fun saveKnownTables(tables: KnownTablesEntity)

    @Query("SELECT * FROM sync_outbox ORDER BY n")
    suspend fun outbox(): List<OutboxEntity>

    @Query("DELETE FROM sync_outbox WHERE n <= :upTo")
    suspend fun clearOutbox(upTo: Long)
}

internal fun PurgeEntity.stamp() = Stamp(hlc, device)

/** A clock that resumes from the last value stored, so a wall clock set backwards cannot reissue an older stamp. */
internal suspend fun SyncDao.loadClock(device: String, wallMillis: () -> Long) =
    HybridClock(device, wallMillis, last = lastHlc() ?: 0)

internal suspend fun SyncDao.saveClock(clock: HybridClock) = saveClock(ClockEntity(lastHlc = clock.last))

/** The own log is to be read again from its start on the next import ([OwnLogEntity]); a recovered mark is kept as it is. */
internal suspend fun SyncDao.rereadOwnLog() {
    if (ownLog() == null) putOwnLog(OwnLogEntity(recovered = false))
}

/** The database was made anew after a corrupt one was set aside: it waits for its own log ([OwnLogEntity.recovered]). */
internal suspend fun SyncDao.markRecovered() = putOwnLog(OwnLogEntity(recovered = true, backupsHeld = true))

/**
 * A backup was restored into the database: it is no longer the near-empty one a recovery left, so backups go on. The
 * wait for the own log stays as it is ([OwnLogEntity.recovered]).
 */
internal suspend fun SyncDao.backupRestored() {
    ownLog()?.takeIf { it.backupsHeld }?.let { putOwnLog(it.copy(backupsHeld = false)) }
}

internal fun BaseEntity.stamp() = Stamp(hlc, device)

internal data class AskKey(val id: String, val grp: String)
