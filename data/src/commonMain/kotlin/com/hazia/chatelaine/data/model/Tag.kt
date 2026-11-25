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

/** The tag table's name. */
const val TAG = "tag"

internal const val PARENT = "parent"
internal const val KIND = "kind"
internal const val KEPT_TOGETHER = "keptTogether"

/** A module's tag carries its key here (R151). */
internal const val MODULE = "module"
internal const val SUGGESTED = "suggested"

/**
 * A tag (R34): at most one parent (R35), at most one kind (R37). The parent, kind and home place are
 * ids that are not foreign keys: a peer's row may arrive before the row it names, and a tag outlives
 * its parent's deletion by moving up.
 */
@Entity(tableName = TAG)
internal data class TagEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_hlc") val nameHlc: Long,
    @ColumnInfo(name = "name_device") val nameDevice: String,
    @ColumnInfo(name = "parent_id") val parentId: String?,
    @ColumnInfo(name = "parent_hlc") val parentHlc: Long,
    @ColumnInfo(name = "parent_device") val parentDevice: String,
    val kind: String?,
    @ColumnInfo(name = "kind_hlc") val kindHlc: Long,
    @ColumnInfo(name = "kind_device") val kindDevice: String,
    val kept: Boolean,
    @ColumnInfo(name = "home_place_id") val homePlaceId: String?,
    @ColumnInfo(name = "kept_together_hlc") val keptTogetherHlc: Long,
    @ColumnInfo(name = "kept_together_device") val keptTogetherDevice: String,
    @ColumnInfo(name = "module_key") val moduleKey: String?,
    @ColumnInfo(name = "module_hlc") val moduleHlc: Long,
    @ColumnInfo(name = "module_device") val moduleDevice: String,
    val suggested: Boolean,
    @ColumnInfo(name = "suggested_hlc") val suggestedHlc: Long,
    @ColumnInfo(name = "suggested_device") val suggestedDevice: String,
)

@Dao
internal interface TagDao {
    @Query("SELECT * FROM tag WHERE id IN (:ids)")
    suspend fun tags(ids: List<String>): List<TagEntity>

    @Query("SELECT * FROM tag ORDER BY id")
    suspend fun allTags(): List<TagEntity>

    @Query("SELECT * FROM tag WHERE id = :id")
    suspend fun tag(id: String): TagEntity?

    @Query("SELECT * FROM tag WHERE parent_id = :parentId ORDER BY id")
    suspend fun childrenOf(parentId: String): List<TagEntity>

    @Query("SELECT * FROM tag WHERE kind = :kindId ORDER BY id")
    suspend fun tagsOfKind(kindId: String): List<TagEntity>

    @Query("SELECT * FROM tag WHERE name = :name COLLATE NOCASE ORDER BY id")
    suspend fun named(name: String): List<TagEntity>

    @Upsert
    suspend fun putTags(rows: List<TagEntity>)

    @Query("DELETE FROM tag WHERE id IN (:ids)")
    suspend fun deleteTags(ids: List<String>)
}

internal fun tagTable(dao: TagDao) =
    EntityTable(dao::tags, dao::allTags, dao::putTags, dao::deleteTags, TagEntity::toRow, Row::toTagEntity) { emptyList() }

internal fun TagEntity.toRow() = Row(TAG, id, mapOf(
    NAME to Group(Stamp(nameHlc, nameDevice), mapOf("name" to name)),
    PARENT to Group(Stamp(parentHlc, parentDevice), mapOf("parentId" to parentId)),
    KIND to Group(Stamp(kindHlc, kindDevice), mapOf("kind" to kind)),
    KEPT_TOGETHER to Group(Stamp(keptTogetherHlc, keptTogetherDevice), mapOf("kept" to kept, "homePlaceId" to homePlaceId)),
    MODULE to Group(Stamp(moduleHlc, moduleDevice), mapOf("moduleKey" to moduleKey)),
    SUGGESTED to Group(Stamp(suggestedHlc, suggestedDevice), mapOf("suggested" to suggested)),
))

/** Throws when [this] is not a tag this version can read. */
internal fun Row.toTagEntity(): TagEntity {
    val name = groups.getValue(NAME)
    val parent = groups.getValue(PARENT)
    val kind = groups.getValue(KIND)
    val kept = groups.getValue(KEPT_TOGETHER)
    val module = groups.getValue(MODULE)
    val suggested = groups.getValue(SUGGESTED)
    return TagEntity(
        id = id,
        name = name.values["name"] as String,
        nameHlc = name.stamp.hlc,
        nameDevice = name.stamp.device,
        parentId = parent.values["parentId"] as String?,
        parentHlc = parent.stamp.hlc,
        parentDevice = parent.stamp.device,
        kind = kind.values["kind"] as String?,
        kindHlc = kind.stamp.hlc,
        kindDevice = kind.stamp.device,
        kept = kept.values["kept"] as Boolean,
        homePlaceId = kept.values["homePlaceId"] as String?,
        keptTogetherHlc = kept.stamp.hlc,
        keptTogetherDevice = kept.stamp.device,
        moduleKey = module.values["moduleKey"] as String?,
        moduleHlc = module.stamp.hlc,
        moduleDevice = module.stamp.device,
        suggested = suggested.values["suggested"] as Boolean,
        suggestedHlc = suggested.stamp.hlc,
        suggestedDevice = suggested.stamp.device,
    )
}

/** A tag as the app reads it. */
data class Tag(
    val id: String,
    val name: String,
    val parentId: String?,
    val kind: String?,
    val kept: Boolean,
    val homePlaceId: String?,
    val moduleKey: String?,
    val suggested: Boolean,
)

internal fun TagEntity.toTag() = Tag(id, name, parentId, kind, kept, homePlaceId, moduleKey, suggested)
