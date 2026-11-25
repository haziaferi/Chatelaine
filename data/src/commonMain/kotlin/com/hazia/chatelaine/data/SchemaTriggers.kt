package com.hazia.chatelaine.data

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * The synced tables, by the name their folder records carry, parents before children;
 * [SchemaTriggers] queues writes to each for export. Empty until the first data table is added.
 */
internal val SYNCED_TABLES: List<String> = emptyList()

/**
 * The triggers Room's annotations cannot declare, dropped and created again on every open so an
 * upgrade never keeps an old body: the sync outbox. Every write to a table in [tables], and every
 * purge, queues the row for export, so no write path can forget to; a deleted row's base and
 * pending questions go too.
 *
 * It also turns foreign keys on for the connection. Room does so itself for a schema with foreign
 * keys; saying it here keeps the rule in force should Room ever stop.
 */
internal class SchemaTriggers(private val tables: List<String> = SYNCED_TABLES) : RoomDatabase.Callback() {

    private val statements: List<String> = buildList {
        for (table in tables) for (event in listOf("INSERT", "UPDATE", "DELETE")) {
            val name = "${table}_outbox_${event.lowercase()}"
            val row = if (event == "DELETE") "OLD" else "NEW"
            // A deleted row, by any path including a foreign key's cascade, takes its local merge state with it.
            val cleanup = if (event == "DELETE") "DELETE FROM sync_base WHERE id = OLD.id; DELETE FROM sync_ask WHERE id = OLD.id;" else ""
            add("DROP TRIGGER IF EXISTS $name")
            add("CREATE TRIGGER $name AFTER $event ON $table BEGIN INSERT INTO sync_outbox(id, tbl) VALUES ($row.id, '$table'); $cleanup END")
        }
        for (event in listOf("INSERT", "UPDATE")) {
            val name = "purge_outbox_${event.lowercase()}"
            add("DROP TRIGGER IF EXISTS $name")
            add("CREATE TRIGGER $name AFTER $event ON purge_registry BEGIN INSERT INTO sync_outbox(id, tbl) VALUES (NEW.id, NULL); END")
        }
    }

    override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        statements.forEach(connection::execSQL)
    }
}
