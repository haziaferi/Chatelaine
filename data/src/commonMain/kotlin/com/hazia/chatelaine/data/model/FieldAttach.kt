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

/** The field-attachment table's name. */
const val FIELD_ATTACH = "field_attach"

/**
 * Where a field is attached (R46): to a tag, to one item, to one wish, or to every item. Exactly one of [tagId],
 * [itemId], [wishId] is set, or [everyItem] is true. The row's id comes from the pair of field and target
 * ([attachId]), so two devices that attach the same field to the same place make one row. The tag, item and wish
 * are ids that are not foreign keys: a peer's row may arrive before the one it names, and an attachment to a tag
 * that is gone reaches nothing. The wish is named by an id only; wishes come with the Shopping module.
 */
@Entity(
    tableName = FIELD_ATTACH,
    foreignKeys = [
        ForeignKey(FieldDefEntity::class, ["id"], ["def_id"], onDelete = ForeignKey.CASCADE, deferred = true),
    ],
    indices = [Index("def_id"), Index("tag_id"), Index("item_id")],
)
internal data class FieldAttachEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "def_id") val defId: String,
    @ColumnInfo(name = "tag_id") val tagId: String?,
    @ColumnInfo(name = "item_id") val itemId: String?,
    @ColumnInfo(name = "wish_id") val wishId: String?,
    @ColumnInfo(name = "every_item") val everyItem: Boolean,
    @ColumnInfo(name = "target_hlc") val targetHlc: Long,
    @ColumnInfo(name = "target_device") val targetDevice: String,
)

@Dao
internal interface FieldAttachDao {
    @Query("SELECT * FROM field_attach WHERE id IN (:ids)")
    suspend fun attaches(ids: List<String>): List<FieldAttachEntity>

    @Query("SELECT * FROM field_attach ORDER BY id")
    suspend fun allAttaches(): List<FieldAttachEntity>

    @Query("SELECT * FROM field_attach WHERE tag_id IN (:tagIds) ORDER BY id")
    suspend fun attachesToTags(tagIds: List<String>): List<FieldAttachEntity>

    @Query("SELECT * FROM field_attach WHERE item_id = :itemId ORDER BY id")
    suspend fun attachesToItem(itemId: String): List<FieldAttachEntity>

    @Query("SELECT * FROM field_attach WHERE every_item ORDER BY id")
    suspend fun attachesToEveryItem(): List<FieldAttachEntity>

    /** Removes the attachments to an item or tag that was purged: those ids have no foreign key, so nothing cascades. */
    @Query(
        "DELETE FROM field_attach WHERE item_id IN (SELECT id FROM purge_registry) " +
            "OR tag_id IN (SELECT id FROM purge_registry)",
    )
    suspend fun deleteOfPurgedTargets()

    @Upsert
    suspend fun putAttaches(rows: List<FieldAttachEntity>)

    @Query("DELETE FROM field_attach WHERE id IN (:ids)")
    suspend fun deleteAttaches(ids: List<String>)
}

internal fun fieldAttachTable(dao: FieldAttachDao) =
    EntityTable(
        dao::attaches, dao::allAttaches, dao::putAttaches, dao::deleteAttaches,
        FieldAttachEntity::toRow, Row::toFieldAttachEntity,
        ownersOf = { listOfNotNull(it.itemId, it.tagId) },
    ) {
        listOf(FIELD_DEF to it.defId)
    }

internal fun FieldAttachEntity.toRow() = Row(FIELD_ATTACH, id, mapOf(
    TARGET to Group(Stamp(targetHlc, targetDevice), mapOf(
        "defId" to defId, "tagId" to tagId, "itemId" to itemId, "wishId" to wishId, "everyItem" to everyItem,
    )),
))

/** Throws when [this] is not an attachment this version can read. */
internal fun Row.toFieldAttachEntity(): FieldAttachEntity {
    val target = groups.getValue(TARGET)
    return FieldAttachEntity(
        id = id,
        defId = target.values["defId"] as String,
        tagId = target.values["tagId"] as String?,
        itemId = target.values["itemId"] as String?,
        wishId = target.values["wishId"] as String?,
        everyItem = target.values["everyItem"] as Boolean,
        targetHlc = target.stamp.hlc,
        targetDevice = target.stamp.device,
    )
}

/**
 * The id of the attachment of [defId] to [target] (`tag:<id>`, `item:<id>`, `wish:<id>` or `every`), the same on
 * every device.
 */
internal fun attachId(defId: String, target: String): String = linkId("attach:$defId", target)
