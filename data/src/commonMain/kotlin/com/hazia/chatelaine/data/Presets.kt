package com.hazia.chatelaine.data

/** Where a preset field attaches (R45's last column, R46). */
enum class PresetAttach {
    /** Added to single items by the person. */
    SINGLE_ITEMS,

    /** Added to single items and wishes by the person, one at a time or in a bulk edit. */
    SINGLE_ITEMS_AND_WISHES,

    /** The tag "wearable" and so every item under it (R151). */
    WEARABLE_TAG,

    /** The tag "consumable" and so every item under it (R151). */
    CONSUMABLE_TAG,

    /** Every item and wish while the module is on. */
    EVERY_ITEM,
}

/**
 * One row of R45's table: a field a module comes with. [choices] are the first options of a choice list, in order;
 * with [fixedChoices] they can be renamed but not added to or removed, since other rules read them (R110, R147, R148).
 * The preset is only data here: turning the module on attaches it.
 */
data class PresetDef(
    val module: String,
    val key: String,
    val name: String,
    val type: FieldType,
    val unit: String? = null,
    val multi: Boolean = false,
    val choices: List<String> = emptyList(),
    val fixedChoices: Boolean = false,
    val attaches: PresetAttach,
)

/** The preset fields of R45. */
object Presets {
    const val INVENTORY = "inventory"
    const val WARDROBE = "wardrobe"
    const val PANTRY = "pantry"
    const val SHOPPING = "shopping"

    val ALL: List<PresetDef> = listOf(
        PresetDef(INVENTORY, "expiry_date", "expiry date", FieldType.DATE, attaches = PresetAttach.SINGLE_ITEMS),
        PresetDef(INVENTORY, "opened_on", "opened on", FieldType.DATE, attaches = PresetAttach.SINGLE_ITEMS),
        PresetDef(INVENTORY, "months_after_opening", "months after opening", FieldType.NUMBER, unit = "months", attaches = PresetAttach.SINGLE_ITEMS),
        PresetDef(WARDROBE, "size", "size", FieldType.TEXT, attaches = PresetAttach.WEARABLE_TAG),
        PresetDef(WARDROBE, "colour", "colour", FieldType.CHOICE, multi = true, attaches = PresetAttach.WEARABLE_TAG),
        PresetDef(WARDROBE, "material", "material", FieldType.CHOICE, multi = true, attaches = PresetAttach.WEARABLE_TAG),
        PresetDef(
            WARDROBE, "season", "season", FieldType.CHOICE, multi = true,
            choices = listOf("spring", "summer", "autumn", "winter"), attaches = PresetAttach.WEARABLE_TAG,
        ),
        PresetDef(WARDROBE, "occasion", "occasion", FieldType.CHOICE, multi = true, attaches = PresetAttach.WEARABLE_TAG),
        PresetDef(WARDROBE, "last_worn", "last worn", FieldType.DATE, attaches = PresetAttach.WEARABLE_TAG),
        PresetDef(
            WARDROBE, "laundry", "laundry", FieldType.CHOICE,
            choices = listOf("clean", "worn", "in the wash"), fixedChoices = true, attaches = PresetAttach.WEARABLE_TAG,
        ),
        PresetDef(
            PANTRY, "counting_unit", "counting unit", FieldType.CHOICE,
            choices = listOf("grams", "millilitres", "pieces", "packs"), fixedChoices = true, attaches = PresetAttach.CONSUMABLE_TAG,
        ),
        // In the counting unit: the unit is the item's own counting unit, so none is stored here.
        PresetDef(PANTRY, "minimum_stock", "minimum stock", FieldType.NUMBER, attaches = PresetAttach.CONSUMABLE_TAG),
        // One pick from the person's category list (R122); shown only while the savings export is on (R125).
        PresetDef(SHOPPING, "category", "category", FieldType.CHOICE, attaches = PresetAttach.EVERY_ITEM),
        PresetDef(SHOPPING, "may_need_bag", "may need a bag", FieldType.CHECKBOX, attaches = PresetAttach.SINGLE_ITEMS_AND_WISHES),
    )

    /** The row a field was made from, or null for a field of the person's own or a row this version does not know. */
    fun of(module: String?, key: String?): PresetDef? = ALL.firstOrNull { it.module == module && it.key == key }
}
