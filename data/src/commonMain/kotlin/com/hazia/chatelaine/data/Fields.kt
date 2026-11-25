package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.Group
import com.hazia.chatelaine.core.sync.Ids
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.sync.readChunked

/**
 * What the app calls to define custom fields (R43, R44), attach them (R46) and fill them in. Every change is one
 * local write, as for [Inventory].
 */
class Fields internal constructor(private val db: ChatelaineDatabase, private val writes: LocalWrites) {

    private val defs get() = db.fieldDefDao()
    private val attaches get() = db.fieldAttachDao()
    private val values get() = db.fieldValueDao()
    private val items get() = db.itemDao()
    private val places get() = db.placeDao()
    private val tags get() = db.tagDao()
    private val links get() = db.itemTagDao()

    // ---- definitions ----

    /**
     * A new field. [unit] is the number's usual unit, [multi] lets a choice list or a link take several, and [options]
     * are the first choices of a choice list; each gets an id that stays when it is renamed (R44, R122).
     */
    suspend fun define(
        name: String,
        type: FieldType,
        unit: String? = null,
        multi: Boolean = false,
        options: List<String> = emptyList(),
    ): String = create(name, type, unit, multi, options, null)

    /**
     * The field R45's [preset] describes, with its choices, not yet attached anywhere: turning its module on does
     * that. A field made from a preset with fixed choices takes no choice added or removed.
     */
    suspend fun definePreset(preset: PresetDef): String =
        create(preset.name, preset.type, preset.unit, preset.multi, preset.choices, preset)

    private suspend fun create(name: String, type: FieldType, unit: String?, multi: Boolean, options: List<String>, preset: PresetDef?): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "a field needs a name" }
        require(unit == null || (type == FieldType.NUMBER && unit.isNotBlank())) { "only a number has a unit" }
        require(!multi || type == FieldType.CHOICE || type == FieldType.LINK) { "only a choice list or a link takes several" }
        require(options.isEmpty() || type == FieldType.CHOICE) { "only a choice list has options" }
        require(options.all { it.isNotBlank() }) { "a choice needs a name" }
        val id = Ids.new()
        val list = options.map { FieldOption(Ids.new(), it.trim()) }
        writes.write(mapOf(FIELD_DEF to listOf(id))) { store ->
            val s = writes.clock.tick()
            writes.merger.created(store, Row(FIELD_DEF, id, mapOf(
                NAME to Group(s, mapOf("name" to trimmed)),
                TYPE to Group(s, mapOf("type" to type.name)),
                UNIT to Group(s, mapOf("unit" to unit)),
                MULTI to Group(s, mapOf("multi" to multi)),
                OPTIONS to Group(s, mapOf("options" to encodeOptions(list))),
                PRESET to Group(s, mapOf("module" to preset?.module, "key" to preset?.key)),
                HIDDEN to Group(s, mapOf("hidden" to false)),
            )))
        }
        return id
    }

    suspend fun getDef(defId: String): FieldDef? = defs.def(defId)?.toFieldDef()

    /** Every field, hidden ones included, by name. */
    suspend fun allDefs(): List<FieldDef> = defs.allDefs().map { it.toFieldDef() }.sortedWith(compareBy({ it.name.lowercase() }, { it.id }))

    /** The choices of a choice list, in order. */
    suspend fun options(defId: String): List<FieldOption> = requireNotNull(defs.def(defId)) { "no field $defId" }.let { parseOptions(it.options) }

    suspend fun rename(defId: String, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "a field needs a name" }
        writes.edit(FIELD_DEF, defId, NAME, { requireNotNull(defs.def(defId)) { "no field $defId" } }) { mapOf("name" to trimmed) }
    }

    /** A hidden field is not offered on items, and keeps its values (R45). */
    suspend fun setHidden(defId: String, hidden: Boolean) =
        writes.edit(FIELD_DEF, defId, HIDDEN, { requireNotNull(defs.def(defId)) { "no field $defId" } }) { mapOf("hidden" to hidden) }

    /** Renames a choice; its id stays, so every value that names it still does (R44, R122). */
    suspend fun renameOption(defId: String, optionId: String, name: String) {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "a choice needs a name" }
        changeOptions(defId, fixedAllowed = true) { list ->
            require(list.any { it.id == optionId }) { "no choice $optionId in field $defId" }
            list.map { if (it.id == optionId) it.copy(name = trimmed) else it }
        }
    }

    /** Adds a choice at the end; refused for a field whose choices are fixed (R45). */
    suspend fun addOption(defId: String, name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "a choice needs a name" }
        val id = Ids.new()
        changeOptions(defId, fixedAllowed = false) { it + FieldOption(id, trimmed) }
        return id
    }

    /**
     * Removes a choice; refused for a field whose choices are fixed (R45). A value that named it stays stored and
     * reads without it.
     */
    suspend fun removeOption(defId: String, optionId: String) =
        changeOptions(defId, fixedAllowed = false) { list ->
            require(list.any { it.id == optionId }) { "no choice $optionId in field $defId" }
            list.filter { it.id != optionId }
        }

    private suspend fun changeOptions(defId: String, fixedAllowed: Boolean, change: (List<FieldOption>) -> List<FieldOption>) =
        writes.write({
            val def = requireNotNull(defs.def(defId)) { "no field $defId" }
            require(def.type == FieldType.CHOICE.name) { "field $defId is not a choice list" }
            require(fixedAllowed || Presets.of(def.presetModule, def.presetKey)?.fixedChoices != true) {
                "the choices of ${def.name} are fixed: they can be renamed, not added to or removed"
            }
            mapOf(FIELD_DEF to listOf(defId))
        }) { store, _ ->
            val row = requireNotNull(store.row(defId)) { "no field $defId" }
            val now = parseOptions(row.groups.getValue(OPTIONS).values["options"] as String)
            store.put(row.edit(OPTIONS, writes.clock.tick(), mapOf("options" to encodeOptions(change(now)))))
        }

    // ---- where a field attaches (R46) ----

    /** Puts [defId] on every item that carries [tagId] or one of its sub-tags, including items tagged later. */
    suspend fun attachToTag(defId: String, tagId: String) =
        attach(defId, "tag:$tagId", { requireNotNull(tags.tag(tagId)) { "no tag $tagId" } }) { it.copy(tagId = tagId) }

    /** Puts [defId] on this one item. */
    suspend fun attachToItem(defId: String, itemId: String) =
        attach(defId, "item:$itemId", { requireNotNull(items.item(itemId)) { "no item $itemId" } }) { it.copy(itemId = itemId) }

    private suspend fun attach(defId: String, target: String, check: suspend () -> Unit, with: (Target) -> Target) {
        val id = attachId(defId, target)
        writes.write({
            requireNotNull(defs.def(defId)) { "no field $defId" }
            check()
            mapOf(FIELD_ATTACH to listOf(id))
        }) { store, _ ->
            // Attached already, on this or another device: one row per pair.
            if (store.row(id) != null) return@write
            val t = with(Target())
            writes.merger.created(store, Row(FIELD_ATTACH, id, mapOf(
                TARGET to Group(writes.clock.tick(), mapOf(
                    "defId" to defId, "tagId" to t.tagId, "itemId" to t.itemId, "wishId" to t.wishId, "everyItem" to t.everyItem,
                )),
            )))
        }
    }

    private data class Target(val tagId: String? = null, val itemId: String? = null, val wishId: String? = null, val everyItem: Boolean = false)

    /**
     * The fields on an item, each once, by name (R46): those attached to a tag the item carries or to any of that
     * tag's ancestors, to the item itself, or to every item. A suggested tag does not count until it is accepted. A
     * hidden field is left out, and so is a field whose tag the item no longer carries: its values stay stored.
     */
    suspend fun fieldsFor(itemId: String): List<FieldDef> {
        val seen = links.linksOfItem(itemId).filter { !it.suggested }.map { it.tagId }.toMutableSet()
        // Tag cycles from two devices are repaired later: the walk stops at a tag it has seen.
        var level = seen.toList()
        while (level.isNotEmpty()) {
            level = readChunked(level) { tags.tags(it) }.mapNotNull { it.parentId }.filter { seen.add(it) }
        }
        val attached = readChunked(seen.toList()) { attaches.attachesToTags(it) } +
            attaches.attachesToItem(itemId) + attaches.attachesToEveryItem()
        val ids = attached.map { it.defId }.distinct()
        return readChunked(ids) { defs.defs(it) }
            .filter { !it.hidden }
            .map { it.toFieldDef() }
            .sortedWith(compareBy({ it.name.lowercase() }, { it.id }))
    }

    // ---- values ----

    /**
     * Sets what [ownerId], an item or a place, holds in [defId]. The input must match the field's type; a choice
     * names options of the field, one unless the field takes several.
     */
    suspend fun set(ownerId: String, defId: String, value: FieldInput) {
        val id = valueId(ownerId, defId)
        val json = encodeInput(value)
        writes.write({
            val def = requireNotNull(defs.def(defId)) { "no field $defId" }.toFieldDef()
            require(items.item(ownerId) != null || places.place(ownerId) != null) { "no item or place $ownerId" }
            check(def, value)
            mapOf(FIELD_VALUE to listOf(id))
        }) { store, _ ->
            val row = store.row(id)
            if (row == null) {
                writes.merger.created(store, Row(FIELD_VALUE, id, mapOf(
                    VALUE to Group(writes.clock.tick(), mapOf("ownerId" to ownerId, "defId" to defId, "json" to json)),
                )))
            } else if (row.groups.getValue(VALUE).values["json"] != json) {
                store.put(row.edit(VALUE, writes.clock.tick(), mapOf("json" to json)))
            }
        }
    }

    private fun check(def: FieldDef, value: FieldInput) {
        val fits = when (value) {
            is FieldInput.Text -> def.type == FieldType.TEXT
            is FieldInput.Number -> def.type == FieldType.NUMBER
            is FieldInput.Date -> def.type == FieldType.DATE
            is FieldInput.Choice -> def.type == FieldType.CHOICE
            is FieldInput.Check -> def.type == FieldType.CHECKBOX
            is FieldInput.Link -> def.type == FieldType.LINK
            is FieldInput.Web -> def.type == FieldType.WEB_LINK
        }
        require(fits) { "${def.name} is a ${def.type} field: it does not take $value" }
        when (value) {
            is FieldInput.Choice -> {
                val known = def.options.map { it.id }.toSet()
                require(value.optionIds.all { it in known }) { "${def.name} has no such choice" }
                require(value.optionIds.distinct().size == value.optionIds.size) { "a choice is picked once" }
                require(def.multi || value.optionIds.size <= 1) { "${def.name} takes one pick" }
            }
            is FieldInput.Link -> require(def.multi || value.ids.size <= 1) { "${def.name} takes one link" }
            is FieldInput.Web -> require(value.url.isNotBlank()) { "a web link needs an address" }
            else -> {}
        }
    }

    /**
     * What [ownerId] holds in [defId], or null when nothing is set. A choice that was removed, and a link to an item
     * or place that is gone, read as not there.
     */
    suspend fun get(ownerId: String, defId: String): FieldInput? {
        val row = values.value(valueId(ownerId, defId)) ?: return null
        return when (val input = decodeInput(row.json)) {
            is FieldInput.Choice -> {
                val known = defs.def(defId)?.let { parseOptions(it.options) }.orEmpty().map { it.id }.toSet()
                FieldInput.Choice(input.optionIds.filter { it in known })
            }
            is FieldInput.Link -> {
                val here = readChunked(input.ids) { items.items(it) }.map { it.id } + readChunked(input.ids) { places.places(it) }.map { it.id }
                FieldInput.Link(input.ids.filter { it in here })
            }
            else -> input
        }
    }
}
