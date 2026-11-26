package com.hazia.chatelaine.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert
import com.hazia.chatelaine.core.sync.Group
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.sync.EntityTable

/** The tag kind table's name. */
const val TAG_KIND = "tag_kind"

/** A kind's position among the kinds. */
internal const val ORDER = "order"

/** A way of sorting tags (R37); the person can rename, add and remove them (R47). */
@Entity(tableName = TAG_KIND)
internal data class TagKindEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_hlc") val nameHlc: Long,
    @ColumnInfo(name = "name_device") val nameDevice: String,
    val position: Long,
    @ColumnInfo(name = "order_hlc") val orderHlc: Long,
    @ColumnInfo(name = "order_device") val orderDevice: String,
)

@Dao
internal interface TagKindDao {
    @Query("SELECT * FROM tag_kind WHERE id IN (:ids)")
    suspend fun kinds(ids: List<String>): List<TagKindEntity>

    @Query("SELECT * FROM tag_kind ORDER BY position, id")
    suspend fun allKinds(): List<TagKindEntity>

    @Upsert
    suspend fun putKinds(rows: List<TagKindEntity>)

    @Query("DELETE FROM tag_kind WHERE id IN (:ids)")
    suspend fun deleteKinds(ids: List<String>)
}

internal fun tagKindTable(dao: TagKindDao) =
    EntityTable(dao::kinds, dao::allKinds, dao::putKinds, dao::deleteKinds, TagKindEntity::toRow, Row::toTagKindEntity) { emptyList() }

internal fun TagKindEntity.toRow() = Row(TAG_KIND, id, mapOf(
    NAME to Group(Stamp(nameHlc, nameDevice), mapOf("name" to name)),
    ORDER to Group(Stamp(orderHlc, orderDevice), mapOf("position" to position)),
))

/** Throws when [this] is not a kind this version can read. */
internal fun Row.toTagKindEntity(): TagKindEntity {
    val name = groups.getValue(NAME)
    val order = groups.getValue(ORDER)
    return TagKindEntity(
        id = id,
        name = name.values["name"] as String,
        nameHlc = name.stamp.hlc,
        nameDevice = name.stamp.device,
        position = order.values["position"] as Long,
        orderHlc = order.stamp.hlc,
        orderDevice = order.stamp.device,
    )
}

/** A kind as the app reads it. */
data class TagKind(val id: String, val name: String, val position: Long)

internal fun TagKindEntity.toKind() = TagKind(id, name, position)

/**
 * The five kinds a new database holds (R38), with fixed ids so every device that makes a new database
 * makes the same rows, and the merge sees one kind, not five more each time a device joins. Their
 * groups carry [NEVER_WRITTEN], so any edit anywhere outranks them.
 */
internal val DEFAULT_KINDS: List<Pair<String, String>> = listOf(
    "kind-function" to "function",
    "kind-project" to "project",
    "kind-season" to "season",
    "kind-place-like" to "place-like",
    "kind-other" to "other",
)

/** The stamp of a row no one has written yet: every real write is later. */
internal val NEVER_WRITTEN = Stamp(0, "")
