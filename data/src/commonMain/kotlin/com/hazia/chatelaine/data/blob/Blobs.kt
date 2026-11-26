package com.hazia.chatelaine.data.blob

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.hazia.chatelaine.data.ChatelaineDatabase

/**
 * When this device last saw a file unreferenced, apart from the folder's copy and its own: what the 30 days of
 * collection count from. Local, never synced.
 */
@Entity(tableName = "blob_seen")
internal data class BlobSeenEntity(
    @PrimaryKey val name: String,
    @ColumnInfo(name = "folder_since") val folderSince: Long?,
    @ColumnInfo(name = "local_since") val localSince: Long?,
)

@Dao
internal interface BlobDao {
    @Query("SELECT * FROM blob_seen") suspend fun seen(): List<BlobSeenEntity>
    @Upsert suspend fun putSeen(rows: List<BlobSeenEntity>)
    @Query("DELETE FROM blob_seen WHERE name IN (:names)") suspend fun clearSeen(names: List<String>)
}

/**
 * The photo files this device keeps (R56). A file is stored once, under the SHA-256 of its bytes; a `photo` row names
 * it by that hash, and [BlobSync] carries the files through the sync folder. The bytes are stored as they are.
 */
class Blobs internal constructor(@Suppress("UNUSED_PARAMETER") db: ChatelaineDatabase, private val store: BlobStore) {

    /**
     * Stores [bytes] and returns their hash, the name a photo row takes (`Inventory.addPhoto`). The same bytes added
     * again are stored once; a copy here that is damaged is replaced.
     */
    fun add(bytes: ByteArray): String {
        val hash = sha256Hex(bytes)
        if (store.read(hash)?.let { sha256Hex(it) } != hash) store.write(hash, bytes)
        return hash
    }

    /**
     * The bytes of the file named [hash], or null: it has not arrived from the device that stored it, or the copy
     * here does not hash to its name. A damaged copy is dropped, so the next sync fetches it again.
     */
    fun read(hash: String): ByteArray? {
        if (!BlobName.isValid(hash)) return null
        val bytes = store.read(hash) ?: return null
        if (sha256Hex(bytes) == hash) return bytes
        store.delete(hash)
        return null
    }
}
