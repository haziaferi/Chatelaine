package com.hazia.chatelaine.data.sync

/**
 * The clock-ahead guard: a record stamped more than [FUTURE_LIMIT_MILLIS] past this device's wall clock is set aside
 * on import, not applied. Applied, it would win every later edit made anywhere until the clocks caught up, and this
 * device's hybrid clock would jump to it. The importer reads it again on the next import, and applies it once it
 * is plausible.
 */
object LineGuard {
    const val FUTURE_LIMIT_MILLIS = 86_400_000L

    /** Whether [record]'s newest stamp is more than a day past [wallMillis]. */
    internal fun isAhead(record: Record, wallMillis: Long): Boolean {
        val newest = when (record) {
            is RowRecord -> record.row.newest
            is PurgeRecord -> record.stamp
        }
        return newest.hlc - wallMillis > FUTURE_LIMIT_MILLIS
    }
}
