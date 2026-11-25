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
