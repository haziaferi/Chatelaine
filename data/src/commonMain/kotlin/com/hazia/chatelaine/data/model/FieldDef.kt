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

/** The field-definition table's name. */
const val FIELD_DEF = "field_def"

internal const val TYPE = "type"
internal const val UNIT = "unit"
internal const val MULTI = "multi"
internal const val PRESET = "preset"
internal const val HIDDEN = "hidden"

/** What a custom field holds (R44). */
enum class FieldType { TEXT, NUMBER, DATE, CHOICE, CHECKBOX, LINK, WEB_LINK }

/** One pick of a choice list; the id stays when the name is changed (R44, R122). */
data class FieldOption(val id: String, val name: String)

/**
 * A custom field (R43, R44), defined once and attached to tags or items. Each part is its own sync group. The
 * choices of a choice list are rows of their own ([FieldOptionEntity]). [presetModule] and [presetKey] name the R45
 * row a preset field was
 * made from, and are null for a field of the person's own.
 */
@Entity(tableName = FIELD_DEF)
internal data class FieldDefEntity(
    @PrimaryKey val id: String,
    val name: String,
    @ColumnInfo(name = "name_hlc") val nameHlc: Long,
    @ColumnInfo(name = "name_device") val nameDevice: String,
    val type: String,
    @ColumnInfo(name = "type_hlc") val typeHlc: Long,
    @ColumnInfo(name = "type_device") val typeDevice: String,
    val unit: String?,
    @ColumnInfo(name = "unit_hlc") val unitHlc: Long,
    @ColumnInfo(name = "unit_device") val unitDevice: String,
    val multi: Boolean,
    @ColumnInfo(name = "multi_hlc") val multiHlc: Long,
    @ColumnInfo(name = "multi_device") val multiDevice: String,
    @ColumnInfo(name = "preset_module") val presetModule: String?,
    @ColumnInfo(name = "preset_key") val presetKey: String?,
    @ColumnInfo(name = "preset_hlc") val presetHlc: Long,
    @ColumnInfo(name = "preset_device") val presetDevice: String,
    val hidden: Boolean,
    @ColumnInfo(name = "hidden_hlc") val hiddenHlc: Long,
    @ColumnInfo(name = "hidden_device") val hiddenDevice: String,
)

@Dao
internal interface FieldDefDao {
    @Query("SELECT * FROM field_def WHERE id IN (:ids)")
    suspend fun defs(ids: List<String>): List<FieldDefEntity>

    @Query("SELECT * FROM field_def ORDER BY id")
    suspend fun allDefs(): List<FieldDefEntity>

    @Query("SELECT * FROM field_def WHERE id = :id")
    suspend fun def(id: String): FieldDefEntity?

    @Upsert
    suspend fun putDefs(rows: List<FieldDefEntity>)

    @Query("DELETE FROM field_def WHERE id IN (:ids)")
    suspend fun deleteDefs(ids: List<String>)
}

internal fun fieldDefTable(dao: FieldDefDao) =
    EntityTable(
        dao::defs, dao::allDefs, dao::putDefs, dao::deleteDefs, FieldDefEntity::toRow, Row::toFieldDefEntity,
    ) { emptyList() }

internal fun FieldDefEntity.toRow() = Row(FIELD_DEF, id, mapOf(
    NAME to Group(Stamp(nameHlc, nameDevice), mapOf("name" to name)),
    TYPE to Group(Stamp(typeHlc, typeDevice), mapOf("type" to type)),
    UNIT to Group(Stamp(unitHlc, unitDevice), mapOf("unit" to unit)),
    MULTI to Group(Stamp(multiHlc, multiDevice), mapOf("multi" to multi)),
    PRESET to Group(Stamp(presetHlc, presetDevice), mapOf("module" to presetModule, "key" to presetKey)),
    HIDDEN to Group(Stamp(hiddenHlc, hiddenDevice), mapOf("hidden" to hidden)),
))

/** Throws when [this] is not a field definition this version can read, such as one of an unknown type. */
internal fun Row.toFieldDefEntity(): FieldDefEntity {
    val name = groups.getValue(NAME)
    val type = groups.getValue(TYPE)
    val unit = groups.getValue(UNIT)
    val multi = groups.getValue(MULTI)
    val preset = groups.getValue(PRESET)
    val hidden = groups.getValue(HIDDEN)
    val typeName = type.values["type"] as String
    FieldType.valueOf(typeName)
    return FieldDefEntity(
        id = id,
        name = name.values["name"] as String,
        nameHlc = name.stamp.hlc,
        nameDevice = name.stamp.device,
        type = typeName,
        typeHlc = type.stamp.hlc,
        typeDevice = type.stamp.device,
        unit = unit.values["unit"] as String?,
        unitHlc = unit.stamp.hlc,
        unitDevice = unit.stamp.device,
        multi = multi.values["multi"] as Boolean,
        multiHlc = multi.stamp.hlc,
        multiDevice = multi.stamp.device,
        presetModule = preset.values["module"] as String?,
        presetKey = preset.values["key"] as String?,
        presetHlc = preset.stamp.hlc,
        presetDevice = preset.stamp.device,
        hidden = hidden.values["hidden"] as Boolean,
        hiddenHlc = hidden.stamp.hlc,
        hiddenDevice = hidden.stamp.device,
    )
}

/** A field definition as the app reads it. A hidden field stays stored with its values (R45). */
data class FieldDef(
    val id: String,
    val name: String,
    val type: FieldType,
    val unit: String?,
    val multi: Boolean,
    val presetModule: String?,
    val presetKey: String?,
    val hidden: Boolean,
)

internal fun FieldDefEntity.toFieldDef() =
    FieldDef(id, name, FieldType.valueOf(type), unit, multi, presetModule, presetKey, hidden)
