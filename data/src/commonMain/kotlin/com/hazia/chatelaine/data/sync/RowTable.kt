package com.hazia.chatelaine.data.sync

import com.hazia.chatelaine.core.sync.Row

/**
 * One synced table as the importer and exporter see it: rows in [Row] form, grouped as the sync rules
 * stamps them. Each table implements it, through [EntityTable], inside the repository's transaction.
 */
internal interface RowTable {
    /** The rows among [ids] this table holds. */
    suspend fun load(ids: Collection<String>): List<Row>

    suspend fun all(): List<Row>

    suspend fun save(rows: Collection<Row>)

    suspend fun delete(ids: Collection<String>)

    /** Whether [row] has this table's groups, fields and value types; one that does not is skipped on import. */
    fun fits(row: Row): Boolean

    /**
     * [row] as an older version wrote it, in this version's groups, with the stamps it came with. Every row read
     * from a folder or a backup goes through it before any merge, so the merge compares like with like whatever
     * order the lines arrive in.
     */
    fun upgrade(row: Row): Row = row

    /** The rows [row] needs present first, as (table, id): what its foreign keys name. */
    fun parents(row: Row): List<Pair<String, String>>

    /**
     * Ids [row] names with no foreign key, and is dead without: a row naming one that was purged is dropped on import.
     * Unlike a parent, one that is not here yet is no reason to wait: the id reads as none until the row arrives.
     */
    fun owners(row: Row): List<String> = emptyList()
}

/**
 * A synced table seen as [Row]s: [toRow] and [fromRow] map its entity [E] to the groups the sync rules stamp and
 * back.
 */
internal class EntityTable<E>(
    private val byIds: suspend (List<String>) -> List<E>,
    private val every: suspend () -> List<E>,
    private val put: suspend (List<E>) -> Unit,
    private val remove: suspend (List<String>) -> Unit,
    private val toRow: (E) -> Row,
    private val fromRow: (Row) -> E,
    private val upgradeRow: (Row) -> Row = { it },
    private val ownersOf: (E) -> List<String> = { emptyList() },
    private val parentsOf: (E) -> List<Pair<String, String>>,
) : RowTable {
    override fun upgrade(row: Row) = upgradeRow(row)
    override suspend fun load(ids: Collection<String>) = byIds(ids.toList()).map(toRow)
    override suspend fun all() = every().map(toRow)
    override suspend fun save(rows: Collection<Row>) = put(rows.map(fromRow))
    override suspend fun delete(ids: Collection<String>) = remove(ids.toList())
    override fun fits(row: Row) = runCatching { fromRow(row) }.isSuccess
    override fun parents(row: Row) = parentsOf(fromRow(row))
    override fun owners(row: Row) = ownersOf(fromRow(row))
}
