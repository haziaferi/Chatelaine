package com.hazia.chatelaine.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.hazia.chatelaine.core.sync.Group
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.sync.EntityTable

/** The item table's name. */
const val ITEM = "item"

/*
 * An item's field groups (R25, R55): one per thing edited apart, so a rename on one device and a
 * new quantity on another both survive the merge.
 */
internal const val NAME = "name"
internal const val NOTES = "notes"
internal const val QUANTITY = "quantity"
internal const val PLACE = "place"
internal const val AWAY = "away"
internal const val PHOTO_OUTDATED = "photoOutdated"

/** A thing the household owns (R25). Only [name] is required (R29). */
@Entity(tableName = ITEM, indices = [Index("place_id")])
internal data class ItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_hlc") val nameHlc: Long,
    @ColumnInfo(name = "name_device") val nameDevice: String,
    val notes: String,
    @ColumnInfo(name = "notes_hlc") val notesHlc: Long,
    @ColumnInfo(name = "notes_device") val notesDevice: String,
    /** How many, or null when not counted; [quantityApprox] makes it "about N" (R26). */
    @ColumnInfo(name = "quantity_count") val quantityCount: Long?,
    @ColumnInfo(name = "quantity_approx") val quantityApprox: Boolean,
    @ColumnInfo(name = "quantity_hlc") val quantityHlc: Long,
    @ColumnInfo(name = "quantity_device") val quantityDevice: String,
    /** A place id; checked against the place table once places exist. */
    @ColumnInfo(name = "place_id") val placeId: String?,
    @ColumnInfo(name = "place_hlc") val placeHlc: Long,
    @ColumnInfo(name = "place_device") val placeDevice: String,
    val away: Boolean,
    @ColumnInfo(name = "away_hlc") val awayHlc: Long,
    @ColumnInfo(name = "away_device") val awayDevice: String,
    @ColumnInfo(name = "photo_outdated") val photoOutdated: Boolean,
    @ColumnInfo(name = "photo_outdated_hlc") val photoOutdatedHlc: Long,
    @ColumnInfo(name = "photo_outdated_device") val photoOutdatedDevice: String,
)

@Dao
internal interface ItemDao {
    @Query("SELECT * FROM item WHERE id IN (:ids)")
    suspend fun items(ids: List<String>): List<ItemEntity>

    @Query("SELECT * FROM item ORDER BY id")
    suspend fun allItems(): List<ItemEntity>

    @Query("SELECT * FROM item WHERE id = :id")
    suspend fun item(id: String): ItemEntity?

    @Upsert
    suspend fun putItems(rows: List<ItemEntity>)

    @Query("DELETE FROM item WHERE id IN (:ids)")
    suspend fun deleteItems(ids: List<String>)
}

internal fun itemTable(dao: ItemDao) =
    EntityTable(dao::items, dao::allItems, dao::putItems, dao::deleteItems, ItemEntity::toRow, Row::toItemEntity) { emptyList() }

internal fun ItemEntity.toRow() = Row(ITEM, id, mapOf(
    NAME to Group(Stamp(nameHlc, nameDevice), mapOf("name" to name)),
    NOTES to Group(Stamp(notesHlc, notesDevice), mapOf("notes" to notes)),
    QUANTITY to Group(Stamp(quantityHlc, quantityDevice), mapOf("count" to quantityCount, "approx" to quantityApprox)),
    PLACE to Group(Stamp(placeHlc, placeDevice), mapOf("placeId" to placeId)),
    AWAY to Group(Stamp(awayHlc, awayDevice), mapOf("away" to away)),
    PHOTO_OUTDATED to Group(Stamp(photoOutdatedHlc, photoOutdatedDevice), mapOf("outdated" to photoOutdated)),
))

/** Throws when [this] is not an item this version can read. */
internal fun Row.toItemEntity(): ItemEntity {
    val name = groups.getValue(NAME)
    val notes = groups.getValue(NOTES)
    val quantity = groups.getValue(QUANTITY)
    val place = groups.getValue(PLACE)
    val away = groups.getValue(AWAY)
    val photo = groups.getValue(PHOTO_OUTDATED)
    return ItemEntity(
        id = id,
        name = name.values["name"] as String,
        nameHlc = name.stamp.hlc,
        nameDevice = name.stamp.device,
        notes = notes.values["notes"] as String,
        notesHlc = notes.stamp.hlc,
        notesDevice = notes.stamp.device,
        quantityCount = quantity.values["count"] as Long?,
        quantityApprox = quantity.values["approx"] as Boolean,
        quantityHlc = quantity.stamp.hlc,
        quantityDevice = quantity.stamp.device,
        placeId = place.values["placeId"] as String?,
        placeHlc = place.stamp.hlc,
        placeDevice = place.stamp.device,
        away = away.values["away"] as Boolean,
        awayHlc = away.stamp.hlc,
        awayDevice = away.stamp.device,
        photoOutdated = photo.values["outdated"] as Boolean,
        photoOutdatedHlc = photo.stamp.hlc,
        photoOutdatedDevice = photo.stamp.device,
    )
}

/** An item as the app reads it. */
data class Item(
    val id: String,
    val name: String,
    val notes: String,
    val count: Long?,
    val approx: Boolean,
    val placeId: String?,
    val away: Boolean,
    val photoOutdated: Boolean,
)

internal fun ItemEntity.toItem() = Item(id, name, notes, quantityCount, quantityApprox, placeId, away, photoOutdated)

/** Where an item stands (R30). "Lent" comes with lending. */
enum class ItemState { UNPLACED, PLACED, AWAY }
