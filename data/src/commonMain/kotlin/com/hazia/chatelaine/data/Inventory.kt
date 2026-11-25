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
    private val places get() = db.placeDao()
    private val placeLinks get() = db.placeTagDao()
    private val photos get() = db.photoDao()
    private val regions get() = db.photoRegionDao()
    private val fieldValues get() = db.fieldValueDao()
    private val fieldAttaches get() = db.fieldAttachDao()

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
        require(count != null || !approx) { "\"about\" needs a number" }
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
        val linkCopies = mutableListOf<LinkCopy>()
        writes.write({
            val item = requireNotNull(items.item(itemId)) { "no item $itemId" }
            require(item.quantityCount == null || item.quantityCount == parts.toLong()) {
                "the quantity ${item.quantityCount} does not split into $parts"
            }
            repeat(parts - 1) { copies += Ids.new() }
            val own = links.linksOfItem(itemId)
            copies.forEach { copy -> own.forEach { linkCopies += LinkCopy(copy, it.tagId, it.suggested) } }
            mapOf(ITEM to listOf(itemId) + copies, ITEM_TAG to linkCopies.map { linkId(it.item, it.tag) })
        }) { store, _ ->
            val s = writes.clock.tick()
            val original = requireNotNull(store.row(itemId))
            store.put(original.edit(QUANTITY, s, mapOf("count" to 1L, "approx" to false)))
            val one = Group(s, mapOf("count" to 1L, "approx" to false))
            for (copy in copies) {
                val groups = original.groups.mapValues { (_, g) -> Group(s, g.values) } + (QUANTITY to one)
                writes.merger.created(store, Row(ITEM, copy, groups))
            }
            for (c in linkCopies) writes.createDerived(store, linkRow(s, c.item, c.tag, c.suggested))
        }
        return listOf(itemId) + copies
    }

    suspend fun state(itemId: String): ItemState {
        val item = requireNotNull(getItem(itemId)) { "no item $itemId" }
        return when {
            item.away -> ItemState.AWAY
            item.placeId != null -> ItemState.PLACED
            else -> ItemState.UNPLACED
        }
    }

    suspend fun setAway(itemId: String, away: Boolean) = writes.edit(ITEM, itemId, AWAY) { mapOf("away" to away) }

    /** Removes the item, its tag links, its photos with their regions, and the regions that point to it, for good (R152). */
    suspend fun deleteItem(itemId: String) {
        val linkIds = mutableListOf<String>()
        val photoIds = mutableListOf<String>()
        val regionIds = mutableSetOf<String>()
        val valueIds = mutableListOf<String>()
        val attachIds = mutableListOf<String>()
        writes.write({
            requireNotNull(items.item(itemId)) { "no item $itemId" }
            valueIds += fieldValues.valuesOfOwner(itemId).map { it.id }
            attachIds += fieldAttaches.attachesToItem(itemId).map { it.id }
            linkIds += links.linksOfItem(itemId).map { it.id }
            photoIds += photos.photosOfItem(itemId).map { it.id }
            photoIds.forEach { p -> regionIds += regions.regionsOfPhoto(p).map { it.id } }
            regionIds += regions.regionsOfItem(itemId).map { it.id }
            mapOf(
                ITEM to listOf(itemId), ITEM_TAG to linkIds.toList(), PHOTO to photoIds.toList(),
                PHOTO_REGION to regionIds.toList(),
                FIELD_VALUE to valueIds.toList(), FIELD_ATTACH to attachIds.toList(),
            )
        }) { store, _ ->
            valueIds.forEach { writes.merger.purge(store, it) }
            attachIds.forEach { writes.merger.purge(store, it) }
            regionIds.forEach { writes.merger.purge(store, it) }
            photoIds.forEach { writes.merger.purge(store, it) }
            linkIds.forEach { writes.merger.purge(store, it) }
            writes.merger.purge(store, itemId)
        }
    }

    suspend fun getItem(itemId: String): Item? = items.item(itemId)?.let { listOf(it).toItems().single() }

    suspend fun items(): List<Item> = items.allItems().toItems()

    // ---- tags ----

    /** A new tag, top-level or under [parentId] (R34, R35). */
    suspend fun addTag(name: String, parentId: String? = null): String = createTag(name, parentId, null)

    /** A module's tag (R151), carrying the module's [moduleKey]. */
    internal suspend fun addModuleTag(name: String, moduleKey: String, parentId: String? = null): String =
        createTag(name, parentId, moduleKey)

    private suspend fun createTag(name: String, parentId: String?, moduleKey: String?): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "a tag needs a name" }
        val id = Ids.new()
        writes.write({
            if (parentId != null) requireNotNull(tags.tag(parentId)) { "no tag $parentId" }
            mapOf(TAG to listOf(id))
        }) { store, _ -> writes.merger.created(store, tagRow(id, writes.clock.tick(), trimmed, parentId, moduleKey)) }
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

    /**
     * Puts [tagId] on every item in [itemIds] at once (R05). An item that has it already is left alone, except
     * that a suggestion of it is accepted: the person tagged it.
     */
    suspend fun tag(itemIds: List<String>, tagId: String) {
        val fresh = mutableListOf<String>() // item ids with no link yet
        val accepted = mutableListOf<String>() // link ids of suggestions the person has now made their own
        writes.write({
            requireNotNull(tags.tag(tagId)) { "no tag $tagId" }
            val ids = itemIds.distinct()
            val found = readChunked(ids) { items.items(it) }.map { it.id }.toSet()
            require(found.size == ids.size) { "no item ${ids.first { it !in found }}" }
            val have = links.linksOfTag(tagId).associateBy { it.itemId }
            ids.filter { it !in have }.forEach { fresh += it }
            ids.mapNotNull { have[it] }.filter { it.suggested }.forEach { accepted += it.id }
            mapOf(ITEM_TAG to fresh.map { linkId(it, tagId) } + accepted)
        }) { store, _ ->
            val s = writes.clock.tick()
            for (item in fresh) {
                // The pair may have been untagged before: its purge ends with the new link.
                writes.createDerived(store, linkRow(s, item, tagId))
            }
            for (id in accepted) store.put(requireNotNull(store.row(id)).edit(SUGGESTED, s, mapOf("suggested" to false)))
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
     * Offers a tag named [name] for [itemId] (R39): the tag is made if no tag has that name, and this item's link
     * to it is marked suggested until the person accepts it. An item that carries the tag already is left as it is.
     */
    suspend fun suggestTag(itemId: String, name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "a tag needs a name" }
        var tagId = ""
        var newTag = false
        var newLink = false
        writes.write({
            requireNotNull(items.item(itemId)) { "no item $itemId" }
            val known = tags.named(trimmed).firstOrNull()
            newTag = known == null
            tagId = known?.id ?: Ids.new()
            newLink = known == null || links.linksOfItem(itemId).none { it.tagId == tagId }
            mapOf(TAG to if (newTag) listOf(tagId) else emptyList(), ITEM_TAG to if (newLink) listOf(linkId(itemId, tagId)) else emptyList())
        }) { store, _ ->
            val s = writes.clock.tick()
            if (newTag) writes.merger.created(store, tagRow(tagId, s, trimmed))
            if (newLink) {
                writes.createDerived(store, linkRow(s, itemId, tagId, suggested = true))
            }
        }
        return tagId
    }

    /** The person keeps the suggestion for [itemId]: its link stops being a suggestion, here only (R39). */
    suspend fun acceptSuggestion(itemId: String, tagId: String) {
        writes.edit(ITEM_TAG, linkId(itemId, tagId), SUGGESTED, {
            require(links.linksOfItem(itemId).any { it.tagId == tagId }) { "item $itemId does not carry tag $tagId" }
        }) { mapOf("suggested" to false) }
    }

    /** Whether [tagId] is on [itemId] only as a suggestion. */
    suspend fun isSuggested(itemId: String, tagId: String): Boolean =
        links.linksOfItem(itemId).firstOrNull { it.tagId == tagId }?.suggested ?: false

    /**
     * Deletes the tag and its links (R152). Its sub-tags move to its parent, or become top-level; its items
     * keep their other tags. A module's tag is deleted the same way: its preset field values are not touched
     * here (they arrive with the modules and stay, hidden).
     */
    suspend fun deleteTag(tagId: String) {
        val childIds = mutableListOf<String>()
        val linkIds = mutableListOf<String>()
        val placeLinkIds = mutableListOf<String>()
        writes.write({
            requireNotNull(tags.tag(tagId)) { "no tag $tagId" }
            childIds += tags.childrenOf(tagId).map { it.id }
            linkIds += links.linksOfTag(tagId).map { it.id }
            placeLinkIds += placeLinks.linksOfTag(tagId).map { it.id }
            mapOf(TAG to listOf(tagId) + childIds, ITEM_TAG to linkIds.toList(), PLACE_TAG to placeLinkIds.toList())
        }) { store, _ ->
            placeLinkIds.forEach { writes.merger.purge(store, it) }
            val up = requireNotNull(store.row(tagId)).groups.getValue(PARENT).values["parentId"] as String?
            val s = writes.clock.tick()
            for (child in childIds) store.put(requireNotNull(store.row(child)).edit(PARENT, s, mapOf("parentId" to up)))
            linkIds.forEach { writes.merger.purge(store, it) }
            writes.merger.purge(store, tagId)
        }
    }

    suspend fun getTag(tagId: String): Tag? = tags.tag(tagId)?.let { listOf(it).toTags().single() }

    suspend fun tags(): List<Tag> = tags.allTags().toTags()

    suspend fun childrenOf(tagId: String): List<Tag> = tags.childrenOf(tagId).toTags()

    /** The tags on an item, by name. */
    suspend fun tagsOf(itemId: String): List<Tag> {
        val ids = links.linksOfItem(itemId).map { it.tagId }
        return readChunked(ids) { tags.tags(it) }.toTags().sortedWith(compareBy({ it.name }, { it.id }))
    }

    /** The items carrying a tag. */
    suspend fun itemsWith(tagId: String): List<Item> {
        val ids = links.linksOfTag(tagId).map { it.itemId }
        return readChunked(ids) { items.items(it) }.toItems().sortedBy { it.id }
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

    // ---- places ----

    /** A new place under [parentId], or at the top (R32); a site is a top-level place of its own. A place is never an item (R111). */
    suspend fun addPlace(name: String, parentId: String? = null, isSite: Boolean = false): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "a place needs a name" }
        require(!isSite || parentId == null) { "a site is at the top of a place tree" }
        val id = Ids.new()
        writes.write({
            if (parentId != null) requireNotNull(places.place(parentId)) { "no place $parentId" }
            mapOf(PLACE_TABLE to listOf(id))
        }) { store, _ -> writes.merger.created(store, placeRow(id, writes.clock.tick(), trimmed, parentId, isSite)) }
        return id
    }

    /** Moves [placeId] under [parentId], or to the top for null; never into itself or one of its places (R32). */
    suspend fun movePlace(placeId: String, parentId: String?) {
        var changes = false
        writes.write({
            val place = requireNotNull(places.place(placeId)) { "no place $placeId" }
            require(!place.isSite || parentId == null) { "a site is at the top of a place tree" }
            if (parentId != null) {
                requireNotNull(places.place(parentId)) { "no place $parentId" }
                val seen = mutableSetOf<String>()
                var up: String? = parentId
                while (up != null && seen.add(up)) {
                    require(up != placeId) { "place $placeId cannot sit inside its own place" }
                    up = places.place(up)?.parentId
                }
            }
            changes = place.parentId != parentId
            mapOf(PLACE_TABLE to if (changes) listOf(placeId) else emptyList())
        }) { store, _ ->
            if (changes) store.put(requireNotNull(store.row(placeId)).edit(PARENT, writes.clock.tick(), mapOf("parentId" to parentId)))
        }
    }

    /** Puts [itemId] in [placeId], or takes it out of any place for null (R30, R111). */
    suspend fun placeItem(itemId: String, placeId: String?) = moveItems(listOf(itemId), placeId)

    /**
     * Puts every item in [itemIds] in [placeId] at once, or takes them out of their places for null. An item
     * moved into or out of a place raises its "photo outdated" flag, and so does each place it left or entered (R31);
     * an item or place with no photo has nothing to be out of date and keeps its flag down.
     */
    suspend fun moveItems(itemIds: List<String>, placeId: String?) {
        val ids = itemIds.distinct()
        val moving = mutableListOf<String>()
        val photographed = mutableSetOf<String>() // the moving items that have a photo
        writes.write({
            if (placeId != null) requireNotNull(places.place(placeId)) { "no place $placeId" }
            val found = readChunked(ids) { items.items(it) }
            require(found.size == ids.size) { "no item ${ids.first { id -> found.none { it.id == id } }}" }
            val shifting = found.filter { it.placeId != placeId }
            moving += shifting.map { it.id }
            photographed += readChunked(moving.toList()) { photos.itemIdsWithPhotos(it) }
            val touched = shifting.mapNotNull { it.placeId }.toMutableSet()
            if (placeId != null && shifting.isNotEmpty()) touched += placeId
            val touchedPlaces = readChunked(touched.toList()) { places.places(it) }.map { it.id }
            mapOf(ITEM to moving.toList(), PLACE_TABLE to readChunked(touchedPlaces) { photos.placeIdsWithPhotos(it) })
        }) { store, targets ->
            val s = writes.clock.tick()
            for (id in moving) {
                val moved = requireNotNull(store.row(id)).edit(PLACE, s, mapOf("placeId" to placeId))
                store.put(if (id in photographed) moved.raised(s) else moved)
            }
            for (id in targets.getValue(PLACE_TABLE)) store.put(requireNotNull(store.row(id)).raised(s))
        }
    }

    /** What [placeId] holds directly: its items and its places (R32, R111). */
    suspend fun contents(placeId: String): PlaceContents {
        requireNotNull(places.place(placeId)) { "no place $placeId" }
        return PlaceContents(places.itemsIn(placeId).toItems(), places.childrenOf(placeId).toPlaces())
    }

    /** The names from the top of the tree down to [placeId] (R32). The walk stops at a place it has met, so a cycle cannot loop it. */
    suspend fun path(placeId: String): List<String> {
        val names = ArrayDeque<String>()
        val seen = mutableSetOf<String>()
        var at: PlaceEntity? = requireNotNull(places.place(placeId)) { "no place $placeId" }
        while (at != null && seen.add(at.id)) {
            names.addFirst(at.name)
            at = at.parentId?.let { places.place(it) }
        }
        return names.toList()
    }

    /**
     * Deletes the place, its tag links, and its photos with their regions (R152). Its items and places move to its parent (a site in it goes to the top); tags that had it as home lose the home; for a site, or a place
     * with no parent, the items become unplaced and the places lose their parent. This is one write, however many
     * items it holds. The places and items moved count as changed, as moving them would (R31).
     */
    suspend fun deletePlace(placeId: String) {
        val childIds = mutableListOf<String>()
        val itemIds = mutableListOf<String>()
        val linkIds = mutableListOf<String>()
        val siteIds = mutableSetOf<String>()
        val homeTagIds = mutableListOf<String>()
        val photoIds = mutableListOf<String>()
        val regionIds = mutableListOf<String>()
        val valueIds = mutableListOf<String>()
        val photographed = mutableSetOf<String>() // the items and the parent place that have a photo
        var up: String? = null
        writes.write({
            val place = requireNotNull(places.place(placeId)) { "no place $placeId" }
            up = place.parentId?.takeIf { !place.isSite && it != placeId && places.place(it) != null }
            places.childrenOf(placeId).forEach { childIds += it.id; if (it.isSite) siteIds += it.id }
            homeTagIds += tags.tagsWithHome(placeId).map { it.id }
            itemIds += places.itemsIn(placeId).map { it.id }
            linkIds += placeLinks.linksOfPlace(placeId).map { it.id }
            photoIds += photos.photosOfPlace(placeId).map { it.id }
            valueIds += fieldValues.valuesOfOwner(placeId).map { it.id }
            photoIds.forEach { p -> regionIds += regions.regionsOfPhoto(p).map { it.id } }
            photographed += readChunked(itemIds.toList()) { photos.itemIdsWithPhotos(it) }
            photographed += readChunked(listOfNotNull(up)) { photos.placeIdsWithPhotos(it) }
            mapOf(
                PLACE_TABLE to (listOf(placeId) + listOfNotNull(up) + childIds).distinct(),
                ITEM to itemIds.toList(),
                TAG to homeTagIds.toList(),
                PLACE_TAG to linkIds.toList(),
                PHOTO to photoIds.toList(),
                PHOTO_REGION to regionIds.toList(),
                FIELD_VALUE to valueIds.toList(),
            )
        }) { store, _ ->
            val s = writes.clock.tick()
            valueIds.forEach { writes.merger.purge(store, it) }
            val parent = up
            regionIds.forEach { writes.merger.purge(store, it) }
            photoIds.forEach { writes.merger.purge(store, it) }
            for (id in itemIds) {
                val moved = requireNotNull(store.row(id)).edit(PLACE, s, mapOf("placeId" to parent))
                store.put(if (id in photographed) moved.raised(s) else moved)
            }
            for (id in childIds) {
                store.put(requireNotNull(store.row(id)).edit(PARENT, s, mapOf("parentId" to parent.takeIf { it != id && id !in siteIds })))
            }
            if (parent != null && parent in photographed && (itemIds.isNotEmpty() || childIds.isNotEmpty())) {
                store.put(requireNotNull(store.row(parent)).raised(s))
            }
            for (id in homeTagIds) store.put(requireNotNull(store.row(id)).edit(HOME, s, mapOf("homePlaceId" to null)))
            linkIds.forEach { writes.merger.purge(store, it) }
            writes.merger.purge(store, placeId)
        }
    }

    suspend fun getPlace(placeId: String): Place? = places.place(placeId)?.let { listOf(it).toPlaces().single() }

    suspend fun places(): List<Place> = places.allPlaces().toPlaces()

    /** Puts [tagId] on [placeId] (R33, R34); a place that carries it already is left alone. */
    suspend fun tagPlace(placeId: String, tagId: String) {
        val id = placeLinkId(placeId, tagId)
        var fresh = false
        writes.write({
            requireNotNull(places.place(placeId)) { "no place $placeId" }
            requireNotNull(tags.tag(tagId)) { "no tag $tagId" }
            fresh = placeLinks.links(listOf(id)).isEmpty()
            mapOf(PLACE_TAG to if (fresh) listOf(id) else emptyList())
        }) { store, _ ->
            if (fresh) {
                // The pair may have been untagged before: its purge ends with the new link.
                writes.createDerived(store, Row(PLACE_TAG, id, mapOf(
                    LINK to Group(writes.clock.tick(), mapOf("placeId" to placeId, "tagId" to tagId)),
                )))
            }
        }
    }

    /** The tags on a place, by name. */
    suspend fun tagsOfPlace(placeId: String): List<Tag> {
        val ids = placeLinks.linksOfPlace(placeId).map { it.tagId }
        return readChunked(ids) { tags.tags(it) }.toTags().sortedWith(compareBy({ it.name }, { it.id }))
    }

    /**
     * Marks [tagId] kept together or not, with its home place (R36). Only a kept-together tag has a home, and the
     * home is a place. The two are groups of their own, so a peer's change to one never overwrites the other.
     */
    suspend fun setKeptTogether(tagId: String, kept: Boolean, homePlaceId: String?) {
        require(kept || homePlaceId == null) { "only a kept-together tag has a home" }
        var keptChanged = false
        var homeChanged = false
        writes.write({
            val tag = requireNotNull(tags.tag(tagId)) { "no tag $tagId" }
            if (homePlaceId != null) requireNotNull(places.place(homePlaceId)) { "no place $homePlaceId" }
            keptChanged = tag.kept != kept
            homeChanged = tag.homePlaceId != homePlaceId
            mapOf(TAG to if (keptChanged || homeChanged) listOf(tagId) else emptyList())
        }) { store, _ ->
            if (keptChanged || homeChanged) {
                val s = writes.clock.tick()
                var row = requireNotNull(store.row(tagId))
                if (keptChanged) row = row.edit(KEPT_TOGETHER, s, mapOf("kept" to kept))
                if (homeChanged) row = row.edit(HOME, s, mapOf("homePlaceId" to homePlaceId))
                store.put(row)
            }
        }
    }

    /**
     * The ids of the items that carry [tagId] or one of its sub-tags and are not in its home place or inside one of
     * the home's places (R36): items that are unplaced or taken out count as away. Empty when the tag is not kept
     * together or its home is not here. The walks down the place and tag trees carry a seen-set.
     */
    suspend fun awayFromHome(tagId: String): List<String> {
        val tag = requireNotNull(tags.tag(tagId)) { "no tag $tagId" }
        val home = tag.homePlaceId
        if (!tag.kept || home == null || places.place(home) == null) return emptyList()
        val inHome = mutableSetOf<String>()
        val placesToVisit = ArrayDeque(listOf(home))
        while (placesToVisit.isNotEmpty()) {
            val at = placesToVisit.removeFirst()
            if (inHome.add(at)) places.childrenOf(at).forEach { placesToVisit.addLast(it.id) }
        }
        val tagIds = mutableSetOf<String>()
        val tagsToVisit = ArrayDeque(listOf(tagId))
        while (tagsToVisit.isNotEmpty()) {
            val at = tagsToVisit.removeFirst()
            if (tagIds.add(at)) tags.childrenOf(at).forEach { tagsToVisit.addLast(it.id) }
        }
        val itemIds = tagIds.flatMap { t -> links.linksOfTag(t).map { it.itemId } }.distinct()
        return readChunked(itemIds) { items.items(it) }
            .filter { item -> val at = item.placeId; item.away || at == null || at !in inHome }
            .map { it.id }
            .sorted()
    }

    // ---- photos ----

    /**
     * Saves a photo on an item or on a place, exactly one of them (R41), naming its picture by [hash], 64 lower-case
     * hex digits (R56). The owner's "photo outdated" flag is cleared (R31).
     */
    suspend fun addPhoto(ownerItemId: String?, ownerPlaceId: String?, hash: String): String {
        require((ownerItemId == null) != (ownerPlaceId == null)) { "a photo is on an item or on a place, not both and not neither" }
        require(HASH.matches(hash)) { "a photo hash is 64 lower-case hex digits" }
        val id = Ids.new()
        val table = if (ownerItemId != null) ITEM else PLACE_TABLE
        val ownerId = ownerItemId ?: requireNotNull(ownerPlaceId)
        writes.write({
            if (ownerItemId != null) requireNotNull(items.item(ownerId)) { "no item $ownerId" }
            else requireNotNull(places.place(ownerId)) { "no place $ownerId" }
            mapOf(table to listOf(ownerId), PHOTO to listOf(id))
        }) { store, _ ->
            val s = writes.clock.tick()
            writes.merger.created(store, Row(PHOTO, id, mapOf(
                OWNER to Group(s, mapOf("itemId" to ownerItemId, "placeId" to ownerPlaceId)),
                BLOB to Group(s, mapOf("hash" to hash)),
                TAKEN to Group(s, mapOf("takenAt" to s.hlc)),
            )))
            val owner = requireNotNull(store.row(ownerId))
            if (owner.groups.getValue(PHOTO_OUTDATED).values["outdated"] == true) {
                store.put(owner.edit(PHOTO_OUTDATED, s, mapOf("outdated" to false)))
            }
        }
        return id
    }

    /**
     * Marks an area of [photoId] that points to [itemId] (R41). [x] and [y] are the area's left and top edge, [w] and
     * [h] its size, all as fractions of the photo, so the area lies inside it and has a size.
     */
    suspend fun addRegion(photoId: String, x: Double, y: Double, w: Double, h: Double, itemId: String): String {
        require(x >= 0 && y >= 0 && w > 0 && h > 0 && x + w <= 1 + EDGE && y + h <= 1 + EDGE) {
            "a region lies inside the photo: x=$x y=$y w=$w h=$h"
        }
        val id = Ids.new()
        writes.write({
            requireNotNull(photos.photo(photoId)) { "no photo $photoId" }
            requireNotNull(items.item(itemId)) { "no item $itemId" }
            mapOf(PHOTO_REGION to listOf(id))
        }) { store, _ ->
            val s = writes.clock.tick()
            writes.merger.created(store, Row(PHOTO_REGION, id, mapOf(
                AREA to Group(s, mapOf("photoId" to photoId, "x" to x, "y" to y, "w" to w, "h" to h)),
                TARGET to Group(s, mapOf("itemId" to itemId)),
            )))
        }
        return id
    }

    /** The item of the smallest region of [photoId] that holds the point, or null (regions may overlap, R41). */
    suspend fun itemAt(photoId: String, x: Double, y: Double): String? =
        regions.regionsOfPhoto(photoId)
            .filter { x >= it.x && x <= it.x + it.w && y >= it.y && y <= it.y + it.h }
            .minWithOrNull(compareBy({ it.w * it.h }, { it.id }))
            ?.itemId
            ?.takeIf { items.item(it) != null }

    // ---- helpers ----

    private companion object {
        val HASH = Regex("[0-9a-f]{64}")

        /** Room for the rounding of an edge that sums to one. */
        const val EDGE = 1e-9
    }

    private class LinkCopy(val item: String, val tag: String, val suggested: Boolean)

    /** The link of a place and a tag: its id never equals an item-tag link's, whatever the ids. */
    private fun placeLinkId(placeId: String, tagId: String) = linkId("place:$placeId", tagId)

    /** This item or place with its "photo outdated" flag raised; as it was when already raised. */
    private fun Row.raised(s: Stamp): Row =
        if (groups.getValue(PHOTO_OUTDATED).values["outdated"] == true) this else edit(PHOTO_OUTDATED, s, mapOf("outdated" to true))

    private fun placeRow(id: String, s: Stamp, name: String, parentId: String?, isSite: Boolean) = Row(PLACE_TABLE, id, mapOf(
        NAME to Group(s, mapOf("name" to name)),
        PARENT to Group(s, mapOf("parentId" to parentId)),
        SITE to Group(s, mapOf("isSite" to isSite)),
        NOTES to Group(s, mapOf("notes" to "")),
        SIZE to Group(s, mapOf("size" to null)),
        MAP_POSITION to Group(s, mapOf("x" to null, "y" to null)),
        PHOTO_OUTDATED to Group(s, mapOf("outdated" to false)),
    ))

    /** Tags as the app reads them: a parent, kind or home place that is not here reads as none. */
    private suspend fun List<TagEntity>.toTags(): List<Tag> {
        val parents = readChunked(mapNotNull { it.parentId }.distinct()) { tags.tags(it) }.map { it.id }.toSet()
        val kindIds = kinds.allKinds().map { it.id }.toSet()
        val homes = existingPlaces(map { it.homePlaceId })
        return map { it.toTag(it.parentId in parents, it.kind in kindIds, it.homePlaceId in homes) }
    }

    private suspend fun existingPlaces(ids: List<String?>): Set<String> =
        readChunked(ids.filterNotNull().distinct()) { places.places(it) }.map { it.id }.toSet()

    /** Items as the app reads them: a place that is not here reads as none. */
    private suspend fun List<ItemEntity>.toItems(): List<Item> {
        val here = existingPlaces(map { it.placeId })
        return map { it.toItem().let { i -> i.copy(placeId = i.placeId?.takeIf { p -> p in here }) } }
    }

    /** Places as the app reads them: a parent that is not here reads as none, so the place lists as top-level. */
    private suspend fun List<PlaceEntity>.toPlaces(): List<Place> {
        val here = existingPlaces(map { it.parentId })
        return map { it.toPlace().let { p -> p.copy(parentId = p.parentId?.takeIf { x -> x in here }) } }
    }

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

    private fun tagRow(id: String, s: Stamp, name: String, parentId: String? = null, moduleKey: String? = null) =
        Row(TAG, id, mapOf(
            NAME to Group(s, mapOf("name" to name)),
            PARENT to Group(s, mapOf("parentId" to parentId)),
            KIND to Group(s, mapOf("kind" to null)),
            KEPT_TOGETHER to Group(s, mapOf("kept" to false)),
            HOME to Group(s, mapOf("homePlaceId" to null)),
            MODULE to Group(s, mapOf("moduleKey" to moduleKey)),
        ))

    private fun linkRow(s: Stamp, itemId: String, tagId: String, suggested: Boolean = false) =
        Row(ITEM_TAG, linkId(itemId, tagId), mapOf(
            LINK to Group(s, mapOf("itemId" to itemId, "tagId" to tagId)),
            SUGGESTED to Group(s, mapOf("suggested" to suggested)),
        ))
}
