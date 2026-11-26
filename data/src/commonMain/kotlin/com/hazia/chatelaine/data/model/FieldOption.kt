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

/** The choice table's name. */
const val FIELD_OPTION = "field_option"

/**
 * One choice of a choice list (R44), a row of its own so that two devices that add, rename or remove different
 * choices at once keep all of it (R55). The id stays when the choice is renamed (R122). A preset's choices have ids
 * derived from the field and the choice's first name, so two devices that make the preset make the same rows; the
 * choices a person adds have random ids. Its name and its place in the list are separate groups.
 */
@Entity(
    tableName = FIELD_OPTION,
    foreignKeys = [
        ForeignKey(FieldDefEntity::class, ["id"], ["def_id"], onDelete = ForeignKey.CASCADE, deferred = true),
    ],
    indices = [Index("def_id")],
)
internal data class FieldOptionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "def_id") val defId: String,
    val name: String,
    @ColumnInfo(name = "name_hlc") val nameHlc: Long,
    @ColumnInfo(name = "name_device") val nameDevice: String,
    val position: Long,
    @ColumnInfo(name = "order_hlc") val orderHlc: Long,
    @ColumnInfo(name = "order_device") val orderDevice: String,
)

@Dao
internal interface FieldOptionDao {
    @Query("SELECT * FROM field_option WHERE id IN (:ids)")
    suspend fun options(ids: List<String>): List<FieldOptionEntity>

    @Query("SELECT * FROM field_option ORDER BY id")
    suspend fun allOptions(): List<FieldOptionEntity>

    @Query("SELECT * FROM field_option WHERE def_id = :defId ORDER BY position, id")
    suspend fun optionsOf(defId: String): List<FieldOptionEntity>

    @Upsert
    suspend fun putOptions(rows: List<FieldOptionEntity>)

    @Query("DELETE FROM field_option WHERE id IN (:ids)")
    suspend fun deleteOptions(ids: List<String>)
}

internal fun fieldOptionTable(dao: FieldOptionDao) =
    EntityTable(
        dao::options, dao::allOptions, dao::putOptions, dao::deleteOptions,
        FieldOptionEntity::toRow, Row::toFieldOptionEntity,
    ) {
        listOf(FIELD_DEF to it.defId)
    }

internal fun FieldOptionEntity.toRow() = Row(FIELD_OPTION, id, mapOf(
    NAME to Group(Stamp(nameHlc, nameDevice), mapOf("defId" to defId, "name" to name)),
    ORDER to Group(Stamp(orderHlc, orderDevice), mapOf("position" to position)),
))

/** Throws when [this] is not a choice this version can read. */
internal fun Row.toFieldOptionEntity(): FieldOptionEntity {
    val name = groups.getValue(NAME)
    val order = groups.getValue(ORDER)
    return FieldOptionEntity(
        id = id,
        defId = name.values["defId"] as String,
        name = name.values["name"] as String,
        nameHlc = name.stamp.hlc,
        nameDevice = name.stamp.device,
        position = order.values["position"] as Long,
        orderHlc = order.stamp.hlc,
        orderDevice = order.stamp.device,
    )
}

/** The id of a preset's choice, the same on every device. */
internal fun presetOptionId(defId: String, firstName: String): String = linkId("option:$defId", firstName)

internal fun FieldOptionEntity.toOption() = FieldOption(id, name)
