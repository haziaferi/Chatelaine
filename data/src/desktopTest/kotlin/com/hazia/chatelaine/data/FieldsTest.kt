package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.HybridClock
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FieldsTest {

    private val dir: File = Files.createTempDirectory("chatelaine-fields").toFile().also { it.deleteOnExit() }
    private val open = openChatelaineDatabase(File(dir, DATABASE_NAME))
    private val db = open.database
    private var now = 1_000L
    private val clock = HybridClock("device-a", { now++ })
    private val writes = LocalWrites(db, clock)
    private val inventory = Inventory(db, writes)
    private val fields = Fields(db, writes)

    @AfterTest
    fun close() = db.close()

    private fun <T> run(block: suspend () -> T): T = runBlocking { block() }

    /** R44: a field of each type, on an item and on a place, is set and read back equal. */
    @Test
    fun eachTypeSurvivesItsRoundTrip() = run {
        val item = inventory.addItem("jar")
        val place = inventory.addPlace("shelf")
        val other = inventory.addItem("lid")
        for (owner in listOf(item, place)) {
            val text = fields.define("note", FieldType.TEXT)
            val number = fields.define("weight", FieldType.NUMBER, unit = "kg")
            val date = fields.define("bought", FieldType.DATE)
            val choice = fields.define("colour", FieldType.CHOICE, multi = true, options = listOf("red", "blue"))
            val check = fields.define("fragile", FieldType.CHECKBOX)
            val link = fields.define("pairs with", FieldType.LINK, multi = true)
            val web = fields.define("manual", FieldType.WEB_LINK)

            val inputs = mapOf(
                text to FieldInput.Text("blue lid"),
                number to FieldInput.Number(2.5, "kg"),
                date to FieldInput.Date(2027, 4, 9),
                choice to FieldInput.Choice(fields.options(choice).map { it.id }),
                check to FieldInput.Check(true),
                link to FieldInput.Link(listOf(other, place)),
                web to FieldInput.Web("https://example.org/manual"),
            )
            for ((def, input) in inputs) fields.set(owner, def, input)
            for ((def, input) in inputs) assertEquals(input, fields.get(owner, def))
            assertEquals(FieldType.entries.toSet(), inputs.keys.map { fields.getDef(it)!!.type }.toSet())
        }
    }

    /** R44: a month and year only stays a month and year. */
    @Test
    fun monthOnlyDateStaysMonthOnly() = run {
        val item = inventory.addItem("cheese")
        val def = fields.define("best before", FieldType.DATE)
        fields.set(item, def, FieldInput.Date(2027, 4, null))
        val back = fields.get(item, def) as FieldInput.Date
        assertEquals(2027, back.year)
        assertEquals(4, back.month)
        assertNull(back.day)
        assertTrue("\"2027-04\"" in db.fieldValueDao().allValues().single().json)

        assertFailsWith<IllegalArgumentException> { FieldInput.Date(2027, 13, null) }
        assertFailsWith<IllegalArgumentException> { FieldInput.Date(2027, 0, null) }
        assertFailsWith<IllegalArgumentException> { FieldInput.Date(2027, 4, 31) }
        assertFailsWith<IllegalArgumentException> { FieldInput.Date(2027, 2, 29) }
        assertEquals(29, FieldInput.Date(2028, 2, 29).day)
    }

    /** R44, R122: renaming a choice changes its name and leaves its id, so a stored value still names it. */
    @Test
    fun renamedChoiceKeepsItsId() = run {
        val item = inventory.addItem("shirt")
        val def = fields.define("season", FieldType.CHOICE, multi = true, options = listOf("spring", "summer"))
        val summer = fields.options(def).first { it.name == "summer" }
        fields.set(item, def, FieldInput.Choice(listOf(summer.id)))

        fields.renameOption(def, summer.id, "hot months")

        val after = fields.options(def)
        assertEquals(listOf("spring", "hot months"), after.map { it.name })
        assertEquals(summer.id, after.first { it.name == "hot months" }.id)
        assertFailsWith<IllegalArgumentException> { fields.renameOption(def, "no-such-option", "x") }
        assertFailsWith<IllegalArgumentException> { fields.set(item, def, FieldInput.Choice(listOf("no-such-option"))) }
        assertEquals(FieldInput.Choice(listOf(summer.id)), fields.get(item, def))
    }

    /** R46: a field on "wearable" reaches an item tagged only with a sub-tag, however deep. */
    @Test
    fun tagFieldReachesSubTagItems() = run {
        val wearable = inventory.addTag("wearable")
        val footwear = inventory.addTag("footwear", wearable)
        val shoes = inventory.addTag("shoes", footwear)
        val size = fields.define("size", FieldType.TEXT)
        fields.attachToTag(size, wearable)
        val item = inventory.addItem("boots")
        val bare = inventory.addItem("spoon")
        assertTrue(fields.fieldsFor(item).isEmpty())

        inventory.tag(listOf(item), shoes)

        assertEquals(listOf(size), fields.fieldsFor(item).map { it.id })
        assertTrue(fields.fieldsFor(bare).isEmpty())
        // Carrying both the tag and its sub-tag gives the field once.
        inventory.tag(listOf(item), wearable)
        assertEquals(listOf(size), fields.fieldsFor(item).map { it.id })
    }

    /** R46: tag cycles from two devices must not hang the walk. */
    @Test
    fun ancestorWalkSurvivesATagCycle() = run {
        val a = inventory.addTag("a")
        val b = inventory.addTag("b", a)
        writes.edit(TAG, a, PARENT) { mapOf("parentId" to b) }
        val def = fields.define("size", FieldType.TEXT)
        fields.attachToTag(def, a)
        val item = inventory.addItem("thing")
        inventory.tag(listOf(item), b)
        assertEquals(listOf(def), fields.fieldsFor(item).map { it.id })
    }

    /** R46: a field added to one item is on that item only. */
    @Test
    fun fieldOnOneItemOnlyThere() = run {
        val a = inventory.addItem("a")
        val b = inventory.addItem("b")
        val def = fields.define("serial", FieldType.TEXT)
        fields.attachToItem(def, a)
        fields.attachToItem(def, a)
        assertEquals(listOf(def), fields.fieldsFor(a).map { it.id })
        assertTrue(fields.fieldsFor(b).isEmpty())
        assertEquals(1, db.fieldAttachDao().allAttaches().size)
    }

    /** R45: the table's fourteen rows. */
    @Test
    fun presetsMatchR45() {
        assertEquals(14, Presets.ALL.size)
        assertEquals(14, Presets.ALL.map { it.module to it.key }.toSet().size)
        assertEquals(
            mapOf("inventory" to 3, "wardrobe" to 7, "pantry" to 2, "shopping" to 2),
            Presets.ALL.groupingBy { it.module }.eachCount(),
        )
        val unit = Presets.ALL.single { it.module == "pantry" && it.key == "counting_unit" }
        assertEquals(listOf("grams", "millilitres", "pieces", "packs"), unit.choices)
        assertTrue(unit.fixedChoices)
        assertFalse(unit.multi)
        val laundry = Presets.ALL.single { it.key == "laundry" }
        assertEquals(listOf("clean", "worn", "in the wash"), laundry.choices)
        assertTrue(laundry.fixedChoices)
        assertEquals(setOf("counting_unit", "laundry"), Presets.ALL.filter { it.fixedChoices }.map { it.key }.toSet())
        val season = Presets.ALL.single { it.key == "season" }
        assertEquals(listOf("spring", "summer", "autumn", "winter"), season.choices)
        assertTrue(season.multi)
        val months = Presets.ALL.single { it.key == "months_after_opening" }
        assertEquals(FieldType.NUMBER, months.type)
        assertEquals("months", months.unit)
        assertEquals(PresetAttach.EVERY_ITEM, Presets.ALL.single { it.key == "category" }.attaches)
        assertEquals(PresetAttach.WEARABLE_TAG, Presets.ALL.single { it.key == "size" }.attaches)

        // Every row: module/key, type, unit, several picks, first choices, fixed choices, where it attaches.
        class Row9(
            val key: String, val type: FieldType, val unit: String?, val multi: Boolean,
            val choices: List<String>, val fixed: Boolean, val attaches: PresetAttach,
        )
        val s = PresetAttach.SINGLE_ITEMS
        val w = PresetAttach.WEARABLE_TAG
        val c = PresetAttach.CONSUMABLE_TAG
        val none = emptyList<String>()
        val expected = mapOf(
            "inventory/expiry_date" to Row9("expiry_date", FieldType.DATE, null, false, none, false, s),
            "inventory/opened_on" to Row9("opened_on", FieldType.DATE, null, false, none, false, s),
            "inventory/months_after_opening" to Row9("months_after_opening", FieldType.NUMBER, "months", false, none, false, s),
            "wardrobe/size" to Row9("size", FieldType.TEXT, null, false, none, false, w),
            "wardrobe/colour" to Row9("colour", FieldType.CHOICE, null, true, none, false, w),
            "wardrobe/material" to Row9("material", FieldType.CHOICE, null, true, none, false, w),
            "wardrobe/season" to Row9("season", FieldType.CHOICE, null, true, season.choices, false, w),
            "wardrobe/occasion" to Row9("occasion", FieldType.CHOICE, null, true, none, false, w),
            "wardrobe/last_worn" to Row9("last_worn", FieldType.DATE, null, false, none, false, w),
            "wardrobe/laundry" to Row9("laundry", FieldType.CHOICE, null, false, laundry.choices, true, w),
            "pantry/counting_unit" to Row9("counting_unit", FieldType.CHOICE, null, false, unit.choices, true, c),
            "pantry/minimum_stock" to Row9("minimum_stock", FieldType.NUMBER, null, false, none, false, c),
            "shopping/category" to Row9("category", FieldType.CHOICE, null, false, none, false, PresetAttach.EVERY_ITEM),
            "shopping/may_need_bag" to Row9("may_need_bag", FieldType.CHECKBOX, null, false, none, false, PresetAttach.SINGLE_ITEMS_AND_WISHES),
        )
        for (p in Presets.ALL) {
            val e = expected.getValue("${p.module}/${p.key}")
            assertEquals(e.type, p.type, p.key)
            assertEquals(e.unit, p.unit, p.key)
            assertEquals(e.multi, p.multi, p.key)
            assertEquals(e.choices, p.choices, p.key)
            assertEquals(e.fixed, p.fixedChoices, p.key)
            assertEquals(e.attaches, p.attaches, p.key)
        }
        assertEquals(expected.keys, Presets.ALL.map { "${it.module}/${it.key}" }.toSet())
    }

    /** A number needs no unit, and a preset made twice, here or on another device, is one field. */
    @Test
    fun numberWithoutUnitAndPresetWithOneId() = run {
        val item = inventory.addItem("flour")
        val stock = fields.define("minimum", FieldType.NUMBER)
        fields.set(item, stock, FieldInput.Number(3.0, null))
        assertEquals(FieldInput.Number(3.0, null), fields.get(item, stock))

        val preset = Presets.ALL.single { it.key == "counting_unit" }
        val first = fields.definePreset(preset)
        val second = fields.definePreset(preset)
        assertEquals(first, second)
        assertEquals(1, db.fieldDefDao().allDefs().count { it.presetKey == "counting_unit" })
        assertEquals(4, fields.options(first).size)
    }

    /** A field refuses a value of another type, several picks where it takes one, duplicate choices, and unknown owners. */
    @Test
    fun refusals() = run {
        val item = inventory.addItem("hat")
        val text = fields.define("note", FieldType.TEXT)
        val one = fields.define("size", FieldType.CHOICE, options = listOf("s", "m", "l"))
        val oneLink = fields.define("twin", FieldType.LINK)
        val ids = fields.options(one).map { it.id }
        assertFailsWith<IllegalArgumentException> { fields.set(item, text, FieldInput.Check(true)) }
        assertFailsWith<IllegalArgumentException> { fields.set(item, one, FieldInput.Choice(ids.take(2))) }
        assertFailsWith<IllegalArgumentException> { fields.set(item, oneLink, FieldInput.Link(listOf(item, item))) }
        assertFailsWith<IllegalArgumentException> { fields.set("no-owner", text, FieldInput.Text("x")) }
        assertFailsWith<IllegalArgumentException> { fields.define("bad", FieldType.CHOICE, options = listOf("a", " A ")) }
        assertEquals(" kg ".trim(), fields.getDef(fields.define("w", FieldType.NUMBER, unit = " kg "))?.unit)
    }

    /** R46: a field attached to every item is on each, read through the attach row (nothing makes one yet). */
    @Test
    fun everyItemFieldIsOnEveryItem() = run {
        val def = fields.define("category", FieldType.CHOICE)
        db.fieldAttachDao().putAttaches(listOf(FieldAttachEntity(
            attachId(def, "every"), def, null, null, null, true, 1L, "device-a",
        )))
        val a = inventory.addItem("a")
        val b = inventory.addItem("b")
        assertEquals(listOf(def), fields.fieldsFor(a).map { it.id })
        assertEquals(listOf(def), fields.fieldsFor(b).map { it.id })
    }

    /** R152: deleting an item takes its values, and the attachments to it, with it. */
    @Test
    fun deletingAnItemPurgesItsFieldRows() = run {
        val item = inventory.addItem("lamp")
        val def = fields.define("note", FieldType.TEXT)
        fields.attachToItem(def, item)
        fields.set(item, def, FieldInput.Text("x"))
        val rows = listOf(valueId(item, def), attachId(def, "item:$item"))

        inventory.deleteItem(item)

        assertTrue(db.fieldValueDao().allValues().isEmpty())
        assertTrue(db.fieldAttachDao().allAttaches().isEmpty())
        assertTrue(db.syncDao().purges().map { it.id }.containsAll(rows))
    }

    /** R55: A adds a choice while B renames another; after the merge both changes are there. */
    @Test
    fun concurrentOptionEditsBothSurvive() = run {
        val def = fields.define("colour", FieldType.CHOICE, multi = true, options = listOf("red", "blue"))
        val red = fields.options(def).first { it.name == "red" }
        fields.addOption(def, "green")
        val row = db.syncedTables().getValue(FIELD_OPTION).load(listOf(red.id)).single()
        val theirs = row.edit(NAME, com.hazia.chatelaine.core.sync.Stamp(5_000, "device-b"), mapOf("name" to "crimson"))
        writes.write(mapOf(FIELD_OPTION to listOf(red.id))) { store -> writes.merger.import(store, listOf(theirs), emptyMap()) }

        assertEquals(listOf("crimson", "blue", "green"), fields.options(def).map { it.name })
        assertEquals(red.id, fields.options(def).first().id)
    }

    /** R152: deleting a place takes its values with it. */
    @Test
    fun deletingAPlacePurgesItsValues() = run {
        val place = inventory.addPlace("shelf")
        val def = fields.define("note", FieldType.TEXT)
        fields.set(place, def, FieldInput.Text("x"))

        inventory.deletePlace(place)

        assertTrue(db.fieldValueDao().allValues().isEmpty())
        assertTrue(valueId(place, def) in db.syncDao().purges().map { it.id })
    }

    /** R45: the choices of the counting unit and of laundry can be renamed, not added to or removed. */
    @Test
    fun fixedChoicesCanBeRenamedNotRemoved() = run {
        val def = fields.definePreset(Presets.ALL.single { it.key == "counting_unit" })
        val grams = fields.options(def).first { it.name == "grams" }

        assertFailsWith<IllegalArgumentException> { fields.removeOption(def, grams.id) }
        assertFailsWith<IllegalArgumentException> { fields.addOption(def, "bunches") }
        fields.renameOption(def, grams.id, "g")
        assertEquals(listOf("g", "millilitres", "pieces", "packs"), fields.options(def).map { it.name })
        assertEquals(grams.id, fields.options(def).first().id)

        // A choice list that is not fixed takes both.
        val season = fields.definePreset(Presets.ALL.single { it.key == "season" })
        val extra = fields.addOption(season, "monsoon")
        fields.removeOption(season, extra)
        assertEquals(listOf("spring", "summer", "autumn", "winter"), fields.options(season).map { it.name })
    }
}
