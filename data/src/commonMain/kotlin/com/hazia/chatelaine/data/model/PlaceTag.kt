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

/** The place-tag link table's name. */
const val PLACE_TAG = "place_tag"

/**
 * One tag on one place (R33, R34): as [ItemTagEntity], a row of its own whose id comes from the pair, so that tagging
 * on one device and untagging on another are two writes the merge keeps apart. Deleting the place or the tag takes
 * its links with it.
 */
@Entity(
    tableName = PLACE_TAG,
    foreignKeys = [
        ForeignKey(PlaceEntity::class, ["id"], ["place_id"], onDelete = ForeignKey.CASCADE, deferred = true),
        ForeignKey(TagEntity::class, ["id"], ["tag_id"], onDelete = ForeignKey.CASCADE, deferred = true),
    ],
    indices = [Index("place_id"), Index("tag_id")],
)
internal data class PlaceTagEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "place_id") val placeId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
    @ColumnInfo(name = "link_hlc") val linkHlc: Long,
    @ColumnInfo(name = "link_device") val linkDevice: String,
)

@Dao
internal interface PlaceTagDao {
    @Query("SELECT * FROM place_tag WHERE id IN (:ids)")
    suspend fun links(ids: List<String>): List<PlaceTagEntity>

    @Query("SELECT * FROM place_tag ORDER BY id")
    suspend fun allLinks(): List<PlaceTagEntity>

    @Query("SELECT * FROM place_tag WHERE tag_id = :tagId ORDER BY id")
    suspend fun linksOfTag(tagId: String): List<PlaceTagEntity>

    @Query("SELECT * FROM place_tag WHERE place_id = :placeId ORDER BY id")
    suspend fun linksOfPlace(placeId: String): List<PlaceTagEntity>

    @Upsert
    suspend fun putLinks(rows: List<PlaceTagEntity>)

    @Query("DELETE FROM place_tag WHERE id IN (:ids)")
    suspend fun deleteLinks(ids: List<String>)
}

internal fun placeTagTable(dao: PlaceTagDao) =
    EntityTable(dao::links, dao::allLinks, dao::putLinks, dao::deleteLinks, PlaceTagEntity::toRow, Row::toPlaceTagEntity) {
        listOf(PLACE_TABLE to it.placeId, TAG to it.tagId)
    }

internal fun PlaceTagEntity.toRow() = Row(PLACE_TAG, id, mapOf(
    LINK to Group(Stamp(linkHlc, linkDevice), mapOf("placeId" to placeId, "tagId" to tagId)),
))

/** Throws when [this] is not a link this version can read. */
internal fun Row.toPlaceTagEntity(): PlaceTagEntity {
    val link = groups.getValue(LINK)
    return PlaceTagEntity(
        id = id,
        placeId = link.values["placeId"] as String,
        tagId = link.values["tagId"] as String,
        linkHlc = link.stamp.hlc,
        linkDevice = link.stamp.device,
    )
}
