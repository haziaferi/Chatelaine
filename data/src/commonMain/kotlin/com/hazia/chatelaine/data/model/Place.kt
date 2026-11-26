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

/** The place table's name. */
const val PLACE_TABLE = "place"

internal const val SITE = "site"
internal const val SIZE = "size"
internal const val MAP_POSITION = "mapPosition"

/**
 * A place (R32, R33, R111): a node of the place tree, never an item. The parent is an id that is not a foreign key:
 * a peer's row may arrive before the one it names, and two devices can each make half of a cycle, which the walks
 * over the tree survive and a repair step ends. An item's place and a tag's home are ids of the same kind.
 */
@Entity(tableName = PLACE_TABLE, indices = [Index("parent_id")])
internal data class PlaceEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_hlc") val nameHlc: Long,
    @ColumnInfo(name = "name_device") val nameDevice: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
    @ColumnInfo(name = "parent_hlc") val parentHlc: Long,
    @ColumnInfo(name = "parent_device") val parentDevice: String,
    /** A site is a top-level place of its own (R32, R152). */
    @ColumnInfo(name = "is_site") val isSite: Boolean,
    @ColumnInfo(name = "site_hlc") val siteHlc: Long,
    @ColumnInfo(name = "site_device") val siteDevice: String,
    val notes: String,
    @ColumnInfo(name = "notes_hlc") val notesHlc: Long,
    @ColumnInfo(name = "notes_device") val notesDevice: String,
    /** How the place is drawn on the map (R33); null when not set. */
    val size: String?,
    @ColumnInfo(name = "size_hlc") val sizeHlc: Long,
    @ColumnInfo(name = "size_device") val sizeDevice: String,
    @ColumnInfo(name = "map_x") val mapX: Double?,
    @ColumnInfo(name = "map_y") val mapY: Double?,
    @ColumnInfo(name = "map_position_hlc") val mapPositionHlc: Long,
    @ColumnInfo(name = "map_position_device") val mapPositionDevice: String,
    @ColumnInfo(name = "photo_outdated") val photoOutdated: Boolean,
    @ColumnInfo(name = "photo_outdated_hlc") val photoOutdatedHlc: Long,
    @ColumnInfo(name = "photo_outdated_device") val photoOutdatedDevice: String,
)

@Dao
internal interface PlaceDao {
    @Query("SELECT * FROM place WHERE id IN (:ids)")
    suspend fun places(ids: List<String>): List<PlaceEntity>

    @Query("SELECT * FROM place ORDER BY id")
    suspend fun allPlaces(): List<PlaceEntity>

    @Query("SELECT * FROM place WHERE id = :id")
    suspend fun place(id: String): PlaceEntity?

    @Query("SELECT * FROM place WHERE parent_id = :parentId ORDER BY id")
    suspend fun childrenOf(parentId: String): List<PlaceEntity>

    @Query("SELECT * FROM item WHERE place_id = :placeId ORDER BY id")
    suspend fun itemsIn(placeId: String): List<ItemEntity>

    @Upsert
    suspend fun putPlaces(rows: List<PlaceEntity>)

    @Query("DELETE FROM place WHERE id IN (:ids)")
    suspend fun deletePlaces(ids: List<String>)
}

internal fun placeTable(dao: PlaceDao) =
    EntityTable(dao::places, dao::allPlaces, dao::putPlaces, dao::deletePlaces, PlaceEntity::toRow, Row::toPlaceEntity) { emptyList() }

internal fun PlaceEntity.toRow() = Row(PLACE_TABLE, id, mapOf(
    NAME to Group(Stamp(nameHlc, nameDevice), mapOf("name" to name)),
    PARENT to Group(Stamp(parentHlc, parentDevice), mapOf("parentId" to parentId)),
    SITE to Group(Stamp(siteHlc, siteDevice), mapOf("isSite" to isSite)),
    NOTES to Group(Stamp(notesHlc, notesDevice), mapOf("notes" to notes)),
    SIZE to Group(Stamp(sizeHlc, sizeDevice), mapOf("size" to size)),
    MAP_POSITION to Group(Stamp(mapPositionHlc, mapPositionDevice), mapOf("x" to mapX, "y" to mapY)),
    PHOTO_OUTDATED to Group(Stamp(photoOutdatedHlc, photoOutdatedDevice), mapOf("outdated" to photoOutdated)),
))

/** Throws when [this] is not a place this version can read. */
internal fun Row.toPlaceEntity(): PlaceEntity {
    val name = groups.getValue(NAME)
    val parent = groups.getValue(PARENT)
    val site = groups.getValue(SITE)
    val notes = groups.getValue(NOTES)
    val size = groups.getValue(SIZE)
    val map = groups.getValue(MAP_POSITION)
    val photo = groups.getValue(PHOTO_OUTDATED)
    return PlaceEntity(
        id = id,
        name = name.values["name"] as String,
        nameHlc = name.stamp.hlc,
        nameDevice = name.stamp.device,
        parentId = parent.values["parentId"] as String?,
        parentHlc = parent.stamp.hlc,
        parentDevice = parent.stamp.device,
        isSite = site.values["isSite"] as Boolean,
        siteHlc = site.stamp.hlc,
        siteDevice = site.stamp.device,
        notes = notes.values["notes"] as String,
        notesHlc = notes.stamp.hlc,
        notesDevice = notes.stamp.device,
        size = size.values["size"] as String?,
        sizeHlc = size.stamp.hlc,
        sizeDevice = size.stamp.device,
        mapX = map.values["x"] as Double?,
        mapY = map.values["y"] as Double?,
        mapPositionHlc = map.stamp.hlc,
        mapPositionDevice = map.stamp.device,
        photoOutdated = photo.values["outdated"] as Boolean,
        photoOutdatedHlc = photo.stamp.hlc,
        photoOutdatedDevice = photo.stamp.device,
    )
}

/** A place as the app reads it. */
data class Place(
    val id: String,
    val name: String,
    val parentId: String?,
    val isSite: Boolean,
    val notes: String,
    val size: String?,
    val x: Double?,
    val y: Double?,
    val photoOutdated: Boolean,
)

internal fun PlaceEntity.toPlace() = Place(id, name, parentId, isSite, notes, size, mapX, mapY, photoOutdated)

/** What a place holds directly: items and places (R32, R111). */
data class PlaceContents(val items: List<Item>, val places: List<Place>)
