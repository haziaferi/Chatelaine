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

/** The item-tag link table's name. */
const val ITEM_TAG = "item_tag"

internal const val LINK = "link"

/**
 * One tag on one item: a row of its own, so that tagging on one device and untagging on another
 * are two writes the merge keeps apart. Untagging purges the row; tagging again makes a new one.
 * The keys are deferred so an import may write a link and its item or tag in either order inside
 * one transaction; deleting an item or tag takes its links with it.
 */
@Entity(
    tableName = ITEM_TAG,
    foreignKeys = [
        ForeignKey(ItemEntity::class, ["id"], ["item_id"], onDelete = ForeignKey.CASCADE, deferred = true),
        ForeignKey(TagEntity::class, ["id"], ["tag_id"], onDelete = ForeignKey.CASCADE, deferred = true),
    ],
    indices = [Index("item_id"), Index("tag_id")],
)
internal data class ItemTagEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "item_id") val itemId: String,
    @ColumnInfo(name = "tag_id") val tagId: String,
    @ColumnInfo(name = "link_hlc") val linkHlc: Long,
    @ColumnInfo(name = "link_device") val linkDevice: String,
)

@Dao
internal interface ItemTagDao {
    @Query("SELECT * FROM item_tag WHERE id IN (:ids)")
    suspend fun links(ids: List<String>): List<ItemTagEntity>

    @Query("SELECT * FROM item_tag ORDER BY id")
    suspend fun allLinks(): List<ItemTagEntity>

    @Query("SELECT * FROM item_tag WHERE item_id = :itemId ORDER BY id")
    suspend fun linksOfItem(itemId: String): List<ItemTagEntity>

    @Query("SELECT * FROM item_tag WHERE tag_id = :tagId ORDER BY id")
    suspend fun linksOfTag(tagId: String): List<ItemTagEntity>

    @Upsert
    suspend fun putLinks(rows: List<ItemTagEntity>)

    @Query("DELETE FROM item_tag WHERE id IN (:ids)")
    suspend fun deleteLinks(ids: List<String>)
}

internal fun itemTagTable(dao: ItemTagDao) =
    EntityTable(dao::links, dao::allLinks, dao::putLinks, dao::deleteLinks, ItemTagEntity::toRow, Row::toItemTagEntity) {
        listOf(ITEM to it.itemId, TAG to it.tagId)
    }

internal fun ItemTagEntity.toRow() = Row(ITEM_TAG, id, mapOf(
    LINK to Group(Stamp(linkHlc, linkDevice), mapOf("itemId" to itemId, "tagId" to tagId)),
))

/** Throws when [this] is not a link this version can read. */
internal fun Row.toItemTagEntity(): ItemTagEntity {
    val link = groups.getValue(LINK)
    return ItemTagEntity(
        id = id,
        itemId = link.values["itemId"] as String,
        tagId = link.values["tagId"] as String,
        linkHlc = link.stamp.hlc,
        linkDevice = link.stamp.device,
    )
}
