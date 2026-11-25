package com.hazia.chatelaine.data.backup

/** When the automatic backup runs (R60): once a day. */
object BackupSchedule {
    const val INTERVAL_MILLIS = 24L * 60 * 60 * 1000

    /** Whether a backup is due: none was made yet ([lastMillis] null), or 24 hours have passed since the last one. */
    fun due(lastMillis: Long?, now: Long): Boolean = lastMillis == null || now - lastMillis >= INTERVAL_MILLIS
}
