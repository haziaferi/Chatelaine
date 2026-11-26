package com.hazia.chatelaine.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.hazia.chatelaine.core.sync.Group
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.sync.EntityTable

/** The photo table's name. */
const val PHOTO = "photo"

internal const val OWNER = "owner"
internal const val BLOB = "blob"
internal const val TAKEN = "taken"

/** What a photo's hash looks like: a SHA-256, 64 lower-case hex digits (R56). */
internal val PHOTO_HASH = Regex("[0-9a-f]{64}")

/**
 * A photo (R41), on an item or on a place and never both: exactly one of [itemId] and [placeId] is set. The photo
 * names its picture by the hash of the content (R56); the files themselves are stored elsewhere. The keys are
 * deferred, as for a link: deleting the item or place takes its photos with it.
 */
@Entity(
    tableName = PHOTO,
    foreignKeys = [
        ForeignKey(ItemEntity::class, ["id"], ["item_id"], onDelete = ForeignKey.CASCADE, deferred = true),
        ForeignKey(PlaceEntity::class, ["id"], ["place_id"], onDelete = ForeignKey.CASCADE, deferred = true),
    ],
    indices = [Index("item_id"), Index("place_id")],
)
internal data class PhotoEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String?,
    @ColumnInfo(name = "place_id") val placeId: String?,
    @ColumnInfo(name = "owner_hlc") val ownerHlc: Long,
    @ColumnInfo(name = "owner_device") val ownerDevice: String,
    /** The SHA-256 of the picture, 64 lower-case hex digits (R56). */
    val hash: String,
    @ColumnInfo(name = "blob_hlc") val blobHlc: Long,
    @ColumnInfo(name = "blob_device") val blobDevice: String,
    /** When the photo was added, in milliseconds. */
    @ColumnInfo(name = "taken_at") val takenAt: Long,
    @ColumnInfo(name = "taken_hlc") val takenHlc: Long,
    @ColumnInfo(name = "taken_device") val takenDevice: String,
)

@Dao
internal interface PhotoDao {
    @Query("SELECT * FROM photo WHERE id IN (:ids)")
    suspend fun photos(ids: List<String>): List<PhotoEntity>

    @Query("SELECT * FROM photo ORDER BY id")
    suspend fun allPhotos(): List<PhotoEntity>

    @Query("SELECT * FROM photo WHERE id = :id")
    suspend fun photo(id: String): PhotoEntity?

    @Query("SELECT * FROM photo WHERE item_id = :itemId ORDER BY id")
    suspend fun photosOfItem(itemId: String): List<PhotoEntity>

    @Query("SELECT * FROM photo WHERE place_id = :placeId ORDER BY id")
    suspend fun photosOfPlace(placeId: String): List<PhotoEntity>

    /** The hash of every photo: the files a device keeps and carries (R56). */
    @Query("SELECT DISTINCT hash FROM photo")
    suspend fun photoHashes(): List<String>

    @Query("SELECT DISTINCT item_id FROM photo WHERE item_id IN (:ids)")
    suspend fun itemIdsWithPhotos(ids: List<String>): List<String>

    @Query("SELECT DISTINCT place_id FROM photo WHERE place_id IN (:ids)")
    suspend fun placeIdsWithPhotos(ids: List<String>): List<String>

    @Upsert
    suspend fun putPhotos(rows: List<PhotoEntity>)

    @Query("DELETE FROM photo WHERE id IN (:ids)")
    suspend fun deletePhotos(ids: List<String>)
}

internal fun photoTable(dao: PhotoDao) =
    EntityTable(
        dao::photos, dao::allPhotos, dao::putPhotos, dao::deletePhotos, PhotoEntity::toRow, Row::toPhotoEntity,
        hasChildren = true, // its regions
    ) {
        listOfNotNull(it.itemId?.let { id -> ITEM to id }, it.placeId?.let { id -> PLACE_TABLE to id })
    }

internal fun PhotoEntity.toRow() = Row(PHOTO, id, mapOf(
    OWNER to Group(Stamp(ownerHlc, ownerDevice), mapOf("itemId" to itemId, "placeId" to placeId)),
    BLOB to Group(Stamp(blobHlc, blobDevice), mapOf("hash" to hash)),
    TAKEN to Group(Stamp(takenHlc, takenDevice), mapOf("takenAt" to takenAt)),
))

/** Throws when [this] is not a photo this version can read. */
internal fun Row.toPhotoEntity(): PhotoEntity {
    val owner = groups.getValue(OWNER)
    val blob = groups.getValue(BLOB)
    val taken = groups.getValue(TAKEN)
    // A peer's row is checked here too: a row that breaks these is not a photo this version can read.
    require((owner.values["itemId"] == null) != (owner.values["placeId"] == null)) { "a photo has exactly one owner" }
    require(PHOTO_HASH.matches(blob.values["hash"] as String)) { "a photo hash is 64 lower-case hex digits" }
    return PhotoEntity(
        id = id,
        itemId = owner.values["itemId"] as String?,
        placeId = owner.values["placeId"] as String?,
        ownerHlc = owner.stamp.hlc,
        ownerDevice = owner.stamp.device,
        hash = blob.values["hash"] as String,
        blobHlc = blob.stamp.hlc,
        blobDevice = blob.stamp.device,
        takenAt = taken.values["takenAt"] as Long,
        takenHlc = taken.stamp.hlc,
        takenDevice = taken.stamp.device,
    )
}
