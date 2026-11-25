package com.hazia.chatelaine.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import com.hazia.chatelaine.data.sync.AskEntity
import com.hazia.chatelaine.data.sync.BaseEntity
import com.hazia.chatelaine.data.sync.ClockEntity
import com.hazia.chatelaine.data.sync.KnownTablesEntity
import com.hazia.chatelaine.data.sync.LostEntity
import com.hazia.chatelaine.data.sync.OutboxEntity
import com.hazia.chatelaine.data.sync.PurgeEntity
import com.hazia.chatelaine.data.sync.ReadEntity
import com.hazia.chatelaine.data.sync.SyncDao
import com.hazia.chatelaine.data.sync.WaitingEntity

/**
 * Chatelaine's one database. For now it holds only the sync tables; the records follow, each with a
 * stamp per field group. It starts at version 1, so there are no migrations yet.
 */
@Database(
    entities = [
        PurgeEntity::class, ClockEntity::class,
        BaseEntity::class, AskEntity::class, ReadEntity::class, OutboxEntity::class, KnownTablesEntity::class,
        WaitingEntity::class, LostEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(ChatelaineDatabaseConstructor::class)
abstract class ChatelaineDatabase : RoomDatabase() {
    internal abstract fun syncDao(): SyncDao
}

// Room's KSP processor generates the actual for each target.
@Suppress("KotlinNoActualForExpect")
expect object ChatelaineDatabaseConstructor : RoomDatabaseConstructor<ChatelaineDatabase>

const val DATABASE_NAME = "chatelaine.db"
