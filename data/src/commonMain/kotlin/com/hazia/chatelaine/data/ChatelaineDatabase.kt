package com.hazia.chatelaine.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.hazia.chatelaine.data.blob.BlobDao
import com.hazia.chatelaine.data.blob.BlobSeenEntity
import com.hazia.chatelaine.data.sync.AskEntity
import com.hazia.chatelaine.data.sync.BaseEntity
import com.hazia.chatelaine.data.sync.ClockEntity
import com.hazia.chatelaine.data.sync.KnownTablesEntity
import com.hazia.chatelaine.data.sync.LostEntity
import com.hazia.chatelaine.data.sync.OutboxEntity
import com.hazia.chatelaine.data.sync.PurgeEntity
import com.hazia.chatelaine.data.sync.ReadEntity
import com.hazia.chatelaine.data.sync.SyncDao
import com.hazia.chatelaine.data.sync.RowTable
import com.hazia.chatelaine.data.sync.WaitingEntity

/**
 * Chatelaine's one database. For now it holds the sync tables and the first records (items, tags, tag kinds and their
 * links), each with a stamp per field group. It starts at version 1, so there are no migrations yet.
 */
@Database(
    entities = [
        PurgeEntity::class, ClockEntity::class,
        BaseEntity::class, AskEntity::class, ReadEntity::class, OutboxEntity::class, KnownTablesEntity::class,
        WaitingEntity::class, LostEntity::class, BlobSeenEntity::class,
        TagKindEntity::class, TagEntity::class, ItemEntity::class, ItemTagEntity::class,
        PlaceEntity::class, PlaceTagEntity::class, PhotoEntity::class, PhotoRegionEntity::class,
        FieldDefEntity::class, FieldOptionEntity::class, FieldAttachEntity::class, FieldValueEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(ChatelaineDatabaseConstructor::class)
abstract class ChatelaineDatabase : RoomDatabase() {
    internal abstract fun syncDao(): SyncDao
    internal abstract fun blobDao(): BlobDao
    internal abstract fun itemDao(): ItemDao
    internal abstract fun tagDao(): TagDao
    internal abstract fun tagKindDao(): TagKindDao
    internal abstract fun itemTagDao(): ItemTagDao
    internal abstract fun placeDao(): PlaceDao
    internal abstract fun placeTagDao(): PlaceTagDao
    internal abstract fun photoDao(): PhotoDao
    internal abstract fun photoRegionDao(): PhotoRegionDao
    internal abstract fun fieldDefDao(): FieldDefDao
    internal abstract fun fieldOptionDao(): FieldOptionDao
    internal abstract fun fieldAttachDao(): FieldAttachDao
    internal abstract fun fieldValueDao(): FieldValueDao
}

/** In [SYNCED_TABLES]' order, parents before children, which is the order a snapshot is written in. */
internal fun ChatelaineDatabase.syncedTables(): Map<String, RowTable> =
    mapOf(
        TAG_KIND to tagKindTable(tagKindDao()),
        TAG to tagTable(tagDao()),
        ITEM to itemTable(itemDao()),
        ITEM_TAG to itemTagTable(itemTagDao()),
        PLACE_TABLE to placeTable(placeDao()),
        PLACE_TAG to placeTagTable(placeTagDao()),
        PHOTO to photoTable(photoDao()),
        PHOTO_REGION to photoRegionTable(photoRegionDao()),
        FIELD_DEF to fieldDefTable(fieldDefDao()),
        FIELD_OPTION to fieldOptionTable(fieldOptionDao()),
        FIELD_ATTACH to fieldAttachTable(fieldAttachDao()),
        FIELD_VALUE to fieldValueTable(fieldValueDao()),
    ).also { check(it.keys.toList() == SYNCED_TABLES) { "every synced table needs its export triggers" } }

/**
 * No group asks a person when both sides changed it: the later stamp wins and the earlier version is kept
 * (R54, R55).
 */
internal val ASK_GROUPS = emptySet<String>()

/** Every user-visible group keeps the version it loses (R54, R55). */
internal val KEEP_LOSER_GROUPS: Set<String> = setOf(
    NAME, NOTES, QUANTITY, PLACE, AWAY, PHOTO_OUTDATED, // item
    PARENT, KIND, KEPT_TOGETHER, HOME, MODULE, // tag
    LINK, SUGGESTED, ORDER, // item_tag, tag_kind
    SITE, SIZE, MAP_POSITION, // place (name, parent, notes and photoOutdated are shared with items and tags)
    OWNER, BLOB, TAKEN, AREA, TARGET, // photo, photo_region, field_attach (target)
    TYPE, UNIT, MULTI, PRESET, HIDDEN, // field_def; field_option has name and order, both shared
    VALUE, // field_value
)

// Room's KSP processor generates the actual for each target.
@Suppress("KotlinNoActualForExpect")
expect object ChatelaineDatabaseConstructor : RoomDatabaseConstructor<ChatelaineDatabase>

const val DATABASE_NAME = "chatelaine.db"
