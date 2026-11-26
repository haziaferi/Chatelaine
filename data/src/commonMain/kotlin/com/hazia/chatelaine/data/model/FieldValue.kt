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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The field-value table's name. */
const val FIELD_VALUE = "field_value"

internal const val VALUE = "value"

/**
 * What one owner (an item or a place, named by an id that is not a foreign key) holds in one field: a row of its own,
 * one per pair ([valueId]), whose single group holds the value as [json]. A value outlives anything that stops showing
 * it: when its field no longer reaches the owner (a module's tag was deleted, R152) it stays here, hidden.
 */
@Entity(
    tableName = FIELD_VALUE,
    foreignKeys = [
        ForeignKey(FieldDefEntity::class, ["id"], ["def_id"], onDelete = ForeignKey.CASCADE, deferred = true),
    ],
    indices = [Index("owner_id"), Index("def_id")],
)
internal data class FieldValueEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "owner_id") val ownerId: String,
    @ColumnInfo(name = "def_id") val defId: String,
    val json: String,
    @ColumnInfo(name = "value_hlc") val valueHlc: Long,
    @ColumnInfo(name = "value_device") val valueDevice: String,
)

@Dao
internal interface FieldValueDao {
    @Query("SELECT * FROM field_value WHERE id IN (:ids)")
    suspend fun values(ids: List<String>): List<FieldValueEntity>

    @Query("SELECT * FROM field_value ORDER BY id")
    suspend fun allValues(): List<FieldValueEntity>

    @Query("SELECT * FROM field_value WHERE id = :id")
    suspend fun value(id: String): FieldValueEntity?

    @Query("SELECT * FROM field_value WHERE owner_id = :ownerId ORDER BY id")
    suspend fun valuesOfOwner(ownerId: String): List<FieldValueEntity>

    @Upsert
    suspend fun putValues(rows: List<FieldValueEntity>)

    @Query("DELETE FROM field_value WHERE id IN (:ids)")
    suspend fun deleteValues(ids: List<String>)
}

internal fun fieldValueTable(dao: FieldValueDao) =
    EntityTable(
        dao::values, dao::allValues, dao::putValues, dao::deleteValues,
        FieldValueEntity::toRow, Row::toFieldValueEntity,
    ) {
        listOf(FIELD_DEF to it.defId)
    }

internal fun FieldValueEntity.toRow() = Row(FIELD_VALUE, id, mapOf(
    VALUE to Group(Stamp(valueHlc, valueDevice), mapOf("ownerId" to ownerId, "defId" to defId, "json" to json)),
))

/** Throws when [this] is not a value this version can read. */
internal fun Row.toFieldValueEntity(): FieldValueEntity {
    val value = groups.getValue(VALUE)
    val json = value.values["json"] as String
    decodeInput(json)
    return FieldValueEntity(
        id = id,
        ownerId = value.values["ownerId"] as String,
        defId = value.values["defId"] as String,
        json = json,
        valueHlc = value.stamp.hlc,
        valueDevice = value.stamp.device,
    )
}

/** The id of [ownerId]'s value in [defId], the same on every device: one row per pair. */
internal fun valueId(ownerId: String, defId: String): String = linkId("value:$ownerId", defId)

/** What a person puts into a field (R44); each kind matches one [FieldType]. */
sealed interface FieldInput {
    data class Text(val s: String) : FieldInput

    /** A number with the unit the person picked or typed; null for none. */
    data class Number(val n: Double, val unit: String?) : FieldInput {
        init {
            require(n.isFinite()) { "a number is finite" }
        }
    }

    /** A full date, or a month and year when [day] is null; stored as `YYYY-MM-DD` or `YYYY-MM`. */
    data class Date(val year: Int, val month: Int, val day: Int?) : FieldInput {
        init {
            require(year in 1..9999) { "year $year" }
            require(month in 1..12) { "month $month" }
            require(day == null || day in 1..daysIn(year, month)) { "day $day of $year-$month" }
        }
    }

    /** The ids of the options picked; one for a field that takes one pick. */
    data class Choice(val optionIds: List<String>) : FieldInput

    data class Check(val on: Boolean) : FieldInput

    /** The ids of the items or places linked. */
    data class Link(val ids: List<String>) : FieldInput

    data class Web(val url: String) : FieldInput
}

private fun daysIn(year: Int, month: Int): Int = when (month) {
    2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
    4, 6, 9, 11 -> 30
    else -> 31
}

private fun two(n: Int) = n.toString().padStart(2, '0')

/** [input] as the JSON a value row stores: `{"k":kind,"v":value}`. */
internal fun encodeInput(input: FieldInput): String {
    val (kind, v) = when (input) {
        is FieldInput.Text -> "text" to JsonPrimitive(input.s)
        is FieldInput.Number ->
            "number" to JsonObject(mapOf("n" to JsonPrimitive(input.n), "u" to JsonPrimitive(input.unit)))
        is FieldInput.Date -> "date" to JsonPrimitive(
            "${input.year.toString().padStart(4, '0')}-${two(input.month)}" + (input.day?.let { "-${two(it)}" } ?: ""),
        )
        is FieldInput.Choice -> "choice" to JsonArray(input.optionIds.map(::JsonPrimitive))
        is FieldInput.Check -> "check" to JsonPrimitive(input.on)
        is FieldInput.Link -> "link" to JsonArray(input.ids.map(::JsonPrimitive))
        is FieldInput.Web -> "web" to JsonPrimitive(input.url)
    }
    return JsonObject(mapOf("k" to JsonPrimitive(kind), "v" to v)).toString()
}

/** Throws when [json] is not a value this version can read. */
internal fun decodeInput(json: String): FieldInput {
    val o = Json.parseToJsonElement(json).jsonObject
    val v = o.getValue("v")
    return when (val kind = o.getValue("k").jsonPrimitive.content) {
        "text" -> FieldInput.Text(v.jsonPrimitive.content)
        "number" -> v.jsonObject.let {
            val unit = it.getValue("u").jsonPrimitive
            FieldInput.Number(it.getValue("n").jsonPrimitive.double, if (unit is JsonNull) null else unit.content)
        }
        "date" -> v.jsonPrimitive.content.split('-').map { it.toInt() }.let {
            require(it.size in 2..3) { "date $v" }
            FieldInput.Date(it[0], it[1], it.getOrNull(2))
        }
        "choice" -> FieldInput.Choice(v.jsonArray.map { it.jsonPrimitive.content })
        "check" -> FieldInput.Check(v.jsonPrimitive.boolean)
        "link" -> FieldInput.Link(v.jsonArray.map { it.jsonPrimitive.content })
        "web" -> FieldInput.Web(v.jsonPrimitive.content)
        else -> throw IllegalArgumentException("unknown value kind $kind")
    }
}
