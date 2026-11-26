package com.hazia.chatelaine.data

import androidx.room.RoomDatabase
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * The synced tables, by the name their folder records carry, parents before children;
 * [SchemaTriggers] queues writes to each for export.
 */
internal val SYNCED_TABLES: List<String> = listOf(
    TAG_KIND, TAG, ITEM, ITEM_TAG, PLACE_TABLE, PLACE_TAG, PHOTO, PHOTO_REGION,
    FIELD_DEF, FIELD_OPTION, FIELD_ATTACH, FIELD_VALUE,
)

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

    /**
     * A new database holds the five default tag kinds (R38). They are written before the triggers exist, so
     * they are not queued: every device makes the same rows (fixed ids, [NEVER_WRITTEN] stamps). Each group
     * also starts with that stamp as its base, so a peer's first edit is not taken for a clash.
     *
     * It also starts with the mark that its own log has not been read back (`OwnLogEntity`): a new database of a
     * device whose log is in the folder reads it before anything compacts that log.
     */
    override fun onCreate(connection: SQLiteConnection) {
        connection.execSQL("INSERT INTO sync_own_log(id, recovered, backups_held) VALUES (0, 0, 0)")
        DEFAULT_KINDS.forEachIndexed { position, (id, name) ->
            connection.execSQL(
                "INSERT INTO tag_kind(id, name, name_hlc, name_device, position, order_hlc, order_device) " +
                    "VALUES ('$id', '$name', ${NEVER_WRITTEN.hlc}, '${NEVER_WRITTEN.device}', $position, ${NEVER_WRITTEN.hlc}, '${NEVER_WRITTEN.device}')",
            )
            for (group in listOf(NAME, ORDER)) {
                connection.execSQL(
                    "INSERT INTO sync_base(id, grp, hlc, device) VALUES ('$id', '$group', ${NEVER_WRITTEN.hlc}, '${NEVER_WRITTEN.device}')",
                )
            }
        }
    }

    override fun onOpen(connection: SQLiteConnection) {
        connection.execSQL("PRAGMA foreign_keys = ON")
        statements.forEach(connection::execSQL)
    }
}
