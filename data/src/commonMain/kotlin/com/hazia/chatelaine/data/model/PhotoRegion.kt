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

/** The photo-region table's name. */
const val PHOTO_REGION = "photo_region"

internal const val AREA = "area"
internal const val TARGET = "target"

/**
 * A marked area of a photo that points to the item it shows (R41): [x], [y], [w] and [h] are fractions of the
 * photo's width and height, the area's left and top edge and its size. The two groups are edited apart, so
 * redrawing the area and pointing it at another item are two writes the merge keeps. Deleting the photo or the
 * item takes its regions with it.
 */
@Entity(
    tableName = PHOTO_REGION,
    foreignKeys = [
        ForeignKey(PhotoEntity::class, ["id"], ["photo_id"], onDelete = ForeignKey.CASCADE, deferred = true),
        ForeignKey(ItemEntity::class, ["id"], ["item_id"], onDelete = ForeignKey.CASCADE, deferred = true),
    ],
    indices = [Index("photo_id"), Index("item_id")],
)
internal data class PhotoRegionEntity(
    @PrimaryKey val id: String,
    /** The photo the region is on; fixed when the region is made, so it sits with the area's group. */
    @ColumnInfo(name = "photo_id") val photoId: String,
    val x: Double,
    val y: Double,
    val w: Double,
    val h: Double,
    @ColumnInfo(name = "area_hlc") val areaHlc: Long,
    @ColumnInfo(name = "area_device") val areaDevice: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "target_hlc") val targetHlc: Long,
    @ColumnInfo(name = "target_device") val targetDevice: String,
)

@Dao
internal interface PhotoRegionDao {
    @Query("SELECT * FROM photo_region WHERE id IN (:ids)")
    suspend fun regions(ids: List<String>): List<PhotoRegionEntity>

    @Query("SELECT * FROM photo_region ORDER BY id")
    suspend fun allRegions(): List<PhotoRegionEntity>

    @Query("SELECT * FROM photo_region WHERE photo_id = :photoId ORDER BY id")
    suspend fun regionsOfPhoto(photoId: String): List<PhotoRegionEntity>

    @Query("SELECT * FROM photo_region WHERE item_id = :itemId ORDER BY id")
    suspend fun regionsOfItem(itemId: String): List<PhotoRegionEntity>

    @Upsert
    suspend fun putRegions(rows: List<PhotoRegionEntity>)

    @Query("DELETE FROM photo_region WHERE id IN (:ids)")
    suspend fun deleteRegions(ids: List<String>)
}

internal fun photoRegionTable(dao: PhotoRegionDao) =
    EntityTable(dao::regions, dao::allRegions, dao::putRegions, dao::deleteRegions, PhotoRegionEntity::toRow, Row::toPhotoRegionEntity) {
        listOf(PHOTO to it.photoId, ITEM to it.itemId)
    }

internal fun PhotoRegionEntity.toRow() = Row(PHOTO_REGION, id, mapOf(
    AREA to Group(Stamp(areaHlc, areaDevice), mapOf("photoId" to photoId, "x" to x, "y" to y, "w" to w, "h" to h)),
    TARGET to Group(Stamp(targetHlc, targetDevice), mapOf("itemId" to itemId)),
))

/** Throws when [this] is not a region this version can read. */
internal fun Row.toPhotoRegionEntity(): PhotoRegionEntity {
    val area = groups.getValue(AREA)
    val target = groups.getValue(TARGET)
    return PhotoRegionEntity(
        id = id,
        photoId = area.values["photoId"] as String,
        x = (area.values["x"] as Number).toDouble(),
        y = (area.values["y"] as Number).toDouble(),
        w = (area.values["w"] as Number).toDouble(),
        h = (area.values["h"] as Number).toDouble(),
        areaHlc = area.stamp.hlc,
        areaDevice = area.stamp.device,
        itemId = target.values["itemId"] as String,
        targetHlc = target.stamp.hlc,
        targetDevice = target.stamp.device,
    )
}
