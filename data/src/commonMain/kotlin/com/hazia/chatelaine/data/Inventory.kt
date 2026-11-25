package com.hazia.chatelaine.data

import com.hazia.chatelaine.core.sync.Group
import com.hazia.chatelaine.core.sync.Ids
import com.hazia.chatelaine.core.sync.Row
import com.hazia.chatelaine.core.sync.Stamp
import com.hazia.chatelaine.data.sync.StagedStore
import com.hazia.chatelaine.data.sync.readChunked

/**
 * What the app calls to read and change items, tags and tag kinds. Every change is one local write
 * (one transaction, one clock tick per edit); a deletion purges the row, so it never comes back from
 * a peer that still holds it.
 */
class Inventory internal constructor(private val db: ChatelaineDatabase, private val writes: LocalWrites) {

    private val items get() = db.itemDao()
    private val tags get() = db.tagDao()
    private val kinds get() = db.tagKindDao()
    private val links get() = db.itemTagDao()

    // ---- items ----

    /** A new item with only a name (R25, R29); with no place it is unplaced (R30). */
    suspend fun addItem(name: String): String {
        require(name.isNotBlank()) { "an item needs a name" }
        val id = Ids.new()
        create(ITEM to listOf(id)) { s -> listOf(itemRow(id, s, name)) }
        return id
    }

    suspend fun rename(itemId: String, name: String) {
        require(name.isNotBlank()) { "an item needs a name" }
        writes.edit(ITEM, itemId, NAME) { mapOf("name" to name) }
    }

    suspend fun setNotes(itemId: String, notes: String) = writes.edit(ITEM, itemId, NOTES) { mapOf("notes" to notes) }

    /** How many; [approx] makes it "about N" (R26). A null [count] is not counted. */
    suspend fun setQuantity(itemId: String, count: Long?, approx: Boolean) {
        require(count == null || count >= 0) { "a quantity is not negative" }
        writes.edit(ITEM, itemId, QUANTITY) { mapOf("count" to count, "approx" to approx) }
    }

    /**
     * Turns one item that stands for [parts] identical things into [parts] items of one each (R28): the item
     * itself is the first, the others copy its name, notes, place, state and tags. The quantity must be
     * [parts] or not counted.
     */
    suspend fun splitItem(itemId: String, parts: Int): List<String> {
        require(parts >= 2) { "splitting makes at least two items" }
        val copies = mutableListOf<String>()
        val linkCopies = mutableListOf<Triple<String, String, String>>() // copy, new link id, tag id
        writes.write({
            val item = requireNotNull(items.item(itemId)) { "no item $itemId" }
            require(item.quantityCount == null || item.quantityCount == parts.toLong()) {
                "the quantity ${item.quantityCount} does not split into $parts"
            }
            repeat(parts - 1) { copies += Ids.new() }
            val tagIds = links.linksOfItem(itemId).map { it.tagId }
            copies.forEach { copy -> tagIds.forEach { linkCopies += Triple(copy, Ids.new(), it) } }
            mapOf(ITEM to listOf(itemId) + copies, ITEM_TAG to linkCopies.map { it.second })
        }) { store, _ ->
            val s = writes.clock.tick()
            val original = requireNotNull(store.row(itemId))
            store.put(original.edit(QUANTITY, s, mapOf("count" to 1L, "approx" to false)))
            val one = Group(s, mapOf("count" to 1L, "approx" to false))
            for (copy in copies) {
                val groups = original.groups.mapValues { (_, g) -> Group(s, g.values) } + (QUANTITY to one)
                writes.merger.created(store, Row(ITEM, copy, groups))
            }
            for ((copy, link, tag) in linkCopies) writes.merger.created(store, linkRow(link, s, copy, tag))
        }
        return listOf(itemId) + copies
    }

    suspend fun state(itemId: String): ItemState {
        val item = requireNotNull(items.item(itemId)) { "no item $itemId" }
        return when {
            item.away -> ItemState.AWAY
            item.placeId != null -> ItemState.PLACED
            else -> ItemState.UNPLACED
        }
    }

    suspend fun setAway(itemId: String, away: Boolean) = writes.edit(ITEM, itemId, AWAY) { mapOf("away" to away) }

    /** Removes the item and its tag links for good (R152). */
    suspend fun deleteItem(itemId: String) {
        val linkIds = mutableListOf<String>()
        writes.write({
            requireNotNull(items.item(itemId)) { "no item $itemId" }
            linkIds += links.linksOfItem(itemId).map { it.id }
            mapOf(ITEM to listOf(itemId), ITEM_TAG to linkIds.toList())
        }) { store, _ ->
            linkIds.forEach { writes.merger.purge(store, it) }
            writes.merger.purge(store, itemId)
        }
    }

    suspend fun getItem(itemId: String): Item? = items.item(itemId)?.toItem()

    suspend fun items(): List<Item> = items.allItems().map { it.toItem() }

    // ---- tags ----

    /** A new tag, top-level or under [parentId] (R34, R35); [moduleKey] marks a module's tag (R151). */
    suspend fun addTag(name: String, parentId: String? = null, moduleKey: String? = null): String {
        require(name.isNotBlank()) { "a tag needs a name" }
        val id = Ids.new()
        writes.write({
            if (parentId != null) requireNotNull(tags.tag(parentId)) { "no tag $parentId" }
            mapOf(TAG to listOf(id))
        }) { store, _ -> writes.merger.created(store, tagRow(id, writes.clock.tick(), name, parentId, moduleKey = moduleKey)) }
        return id
    }

    /** Moves [tagId] under [parentId], or to the top for null. A tag has one parent and never its own descendant (R35). */
    suspend fun setParent(tagId: String, parentId: String?) {
        writes.edit(TAG, tagId, PARENT, {
            if (parentId != null) {
                requireNotNull(tags.tag(parentId)) { "no tag $parentId" }
                val seen = mutableSetOf<String>()
                var up: String? = parentId
                while (up != null && seen.add(up)) {
                    require(up != tagId) { "tag $tagId cannot sit under its own sub-tag" }
                    up = tags.tag(up)?.parentId
                }
            }
        }) { mapOf("parentId" to parentId) }
    }

    /** Sets the tag's kind (R37), or none for null. */
    suspend fun setKind(tagId: String, kindId: String?) {
        writes.edit(TAG, tagId, KIND, {
            if (kindId != null) require(kinds.kinds(listOf(kindId)).isNotEmpty()) { "no kind $kindId" }
        }) { mapOf("kind" to kindId) }
    }

    /** Puts [tagId] on every item in [itemIds] at once; an item that has it already is left alone (R05). */
    suspend fun tag(itemIds: List<String>, tagId: String) {
        val wanted = mutableListOf<Pair<String, String>>() // link id, item id
        writes.write({
            requireNotNull(tags.tag(tagId)) { "no tag $tagId" }
            val ids = itemIds.distinct()
            val found = readChunked(ids) { items.items(it) }.map { it.id }.toSet()
            require(found.size == ids.size) { "no item ${ids.first { it !in found }}" }
            val have = links.linksOfTag(tagId).map { it.itemId }.toSet()
            ids.filter { it !in have }.forEach { wanted += Ids.new() to it }
            mapOf(ITEM_TAG to wanted.map { it.first })
        }) { store, _ ->
            val s = writes.clock.tick()
            for ((link, item) in wanted) writes.merger.created(store, linkRow(link, s, item, tagId))
        }
    }

    /** Takes [tagId] off every item in [itemIds] at once; the links are purged. */
    suspend fun untag(itemIds: List<String>, tagId: String) {
        val doomed = mutableListOf<String>()
        writes.write({
            val ids = itemIds.toSet()
            doomed += links.linksOfTag(tagId).filter { it.itemId in ids }.map { it.id }
            mapOf(ITEM_TAG to doomed.toList())
        }) { store, _ -> doomed.forEach { writes.merger.purge(store, it) } }
    }

    /**
     * Offers a tag named [name] for [itemId] (R39): the tag is marked suggested and the item carries it until the
     * person accepts it. A tag of that name that exists already is used as it is.
     */
    suspend fun suggestTag(itemId: String, name: String): String {
        require(name.isNotBlank()) { "a tag needs a name" }
        var tagId = ""
        var newTag = false
        var newLink: String? = null
        writes.write({
            requireNotNull(items.item(itemId)) { "no item $itemId" }
            val known = tags.named(name.trim()).firstOrNull()
            newTag = known == null
            tagId = known?.id ?: Ids.new()
            val linked = known != null && links.linksOfItem(itemId).any { it.tagId == tagId }
            newLink = if (linked) null else Ids.new()
            mapOf(TAG to if (newTag) listOf(tagId) else emptyList(), ITEM_TAG to listOfNotNull(newLink))
        }) { store, _ ->
            val s = writes.clock.tick()
            if (newTag) writes.merger.created(store, tagRow(tagId, s, name.trim(), suggested = true))
            newLink?.let { writes.merger.created(store, linkRow(it, s, itemId, tagId)) }
        }
        return tagId
    }

    /** The person keeps the suggested tag: it stops being a suggestion and is a tag like any other (R39). */
    suspend fun acceptSuggestion(itemId: String, tagId: String) {
        writes.edit(TAG, tagId, SUGGESTED, {
            requireNotNull(tags.tag(tagId)) { "no tag $tagId" }
            require(links.linksOfItem(itemId).any { it.tagId == tagId }) { "item $itemId does not carry tag $tagId" }
        }) { mapOf("suggested" to false) }
    }

    /**
     * Deletes the tag and its links (R152). Its sub-tags move to its parent, or become top-level; its items
     * keep their other tags. A module's tag is deleted the same way: its preset field values are not touched
     * here (they arrive with the modules and stay, hidden).
     */
    suspend fun deleteTag(tagId: String) {
        val childIds = mutableListOf<String>()
        val linkIds = mutableListOf<String>()
        writes.write({
            requireNotNull(tags.tag(tagId)) { "no tag $tagId" }
            childIds += tags.childrenOf(tagId).map { it.id }
            linkIds += links.linksOfTag(tagId).map { it.id }
            mapOf(TAG to listOf(tagId) + childIds, ITEM_TAG to linkIds.toList())
        }) { store, _ ->
            val up = requireNotNull(store.row(tagId)).groups.getValue(PARENT).values["parentId"] as String?
            val s = writes.clock.tick()
            for (child in childIds) store.put(requireNotNull(store.row(child)).edit(PARENT, s, mapOf("parentId" to up)))
            linkIds.forEach { writes.merger.purge(store, it) }
            writes.merger.purge(store, tagId)
        }
    }

    suspend fun getTag(tagId: String): Tag? = tags.tag(tagId)?.toTag()

    suspend fun tags(): List<Tag> = tags.allTags().map { it.toTag() }

    suspend fun childrenOf(tagId: String): List<Tag> = tags.childrenOf(tagId).map { it.toTag() }

    /** The tags on an item, by name. */
    suspend fun tagsOf(itemId: String): List<Tag> {
        val ids = links.linksOfItem(itemId).map { it.tagId }
        return readChunked(ids) { tags.tags(it) }.map { it.toTag() }.sortedWith(compareBy({ it.name }, { it.id }))
    }

    /** The items carrying a tag. */
    suspend fun itemsWith(tagId: String): List<Item> {
        val ids = links.linksOfTag(tagId).map { it.itemId }
        return readChunked(ids) { items.items(it) }.map { it.toItem() }.sortedBy { it.id }
    }

    // ---- tag kinds ----

    /** A new kind after the others (R47). */
    suspend fun addKind(name: String): String {
        require(name.isNotBlank()) { "a kind needs a name" }
        val id = Ids.new()
        var position = 0L
        writes.write({
            position = (kinds.allKinds().maxOfOrNull { it.position } ?: -1L) + 1
            mapOf(TAG_KIND to listOf(id))
        }) { store, _ ->
            val s = writes.clock.tick()
            writes.merger.created(store, Row(TAG_KIND, id, mapOf(
                NAME to Group(s, mapOf("name" to name)),
                ORDER to Group(s, mapOf("position" to position)),
            )))
        }
        return id
    }

    suspend fun renameKind(kindId: String, name: String) {
        require(name.isNotBlank()) { "a kind needs a name" }
        writes.edit(TAG_KIND, kindId, NAME) { mapOf("name" to name) }
    }

    /** Removes the kind; its tags keep no kind (R47). */
    suspend fun deleteKind(kindId: String) {
        val tagIds = mutableListOf<String>()
        writes.write({
            require(kinds.kinds(listOf(kindId)).isNotEmpty()) { "no kind $kindId" }
            tagIds += tags.tagsOfKind(kindId).map { it.id }
            mapOf(TAG_KIND to listOf(kindId), TAG to tagIds.toList())
        }) { store, _ ->
            val s = writes.clock.tick()
            for (id in tagIds) store.put(requireNotNull(store.row(id)).edit(KIND, s, mapOf("kind" to null)))
            writes.merger.purge(store, kindId)
        }
    }

    /** The kinds in order. */
    suspend fun kinds(): List<TagKind> = kinds.allKinds().map { it.toKind() }

    // ---- helpers ----

    /** Writes rows made with one stamp; [make] gets that stamp. */
    private suspend fun create(target: Pair<String, List<String>>, make: (Stamp) -> List<Row>) =
        writes.write(mapOf(target)) { store: StagedStore ->
            make(writes.clock.tick()).forEach { writes.merger.created(store, it) }
        }

    private fun itemRow(id: String, s: Stamp, name: String) = Row(ITEM, id, mapOf(
        NAME to Group(s, mapOf("name" to name)),
        NOTES to Group(s, mapOf("notes" to "")),
        QUANTITY to Group(s, mapOf("count" to null, "approx" to false)),
        PLACE to Group(s, mapOf("placeId" to null)),
        AWAY to Group(s, mapOf("away" to false)),
        PHOTO_OUTDATED to Group(s, mapOf("outdated" to false)),
    ))

    private fun tagRow(id: String, s: Stamp, name: String, parentId: String? = null, moduleKey: String? = null, suggested: Boolean = false) =
        Row(TAG, id, mapOf(
            NAME to Group(s, mapOf("name" to name)),
            PARENT to Group(s, mapOf("parentId" to parentId)),
            KIND to Group(s, mapOf("kind" to null)),
            KEPT_TOGETHER to Group(s, mapOf("kept" to false, "homePlaceId" to null)),
            MODULE to Group(s, mapOf("moduleKey" to moduleKey)),
            SUGGESTED to Group(s, mapOf("suggested" to suggested)),
        ))

    private fun linkRow(id: String, s: Stamp, itemId: String, tagId: String) =
        Row(ITEM_TAG, id, mapOf(LINK to Group(s, mapOf("itemId" to itemId, "tagId" to tagId))))
}
