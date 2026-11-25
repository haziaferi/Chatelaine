package com.hazia.chatelaine

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.hazia.chatelaine.data.backup.Backup
import com.hazia.chatelaine.data.sync.SyncFolder
import java.util.concurrent.TimeUnit

/** What a daily backup needs: the [Backup] and the folder the user picked for it. */
class BackupJob(val backup: Backup, val folder: SyncFolder)

/**
 * Makes today's backup in the picked folder and keeps the last seven (R60). Returns the file's name, or null when
 * [job] gives none: no backup folder is picked, which is not an error.
 */
suspend fun runBackup(job: () -> BackupJob?, nowMillis: Long): String? =
    job()?.let { it.backup.daily(it.folder, nowMillis) }

/**
 * The daily backup, run by WorkManager every 24 hours (R60). No settings screen picks the folder yet (stage 2), so
 * [job] answers null until the app sets it, and the work then does nothing.
 */
class BackupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        runBackup(job, System.currentTimeMillis())
        return Result.success()
    }

    companion object {
        const val UNIQUE_NAME = "chatelaine-daily-backup"

        /** Where the worker finds the backup to make; set by the app once a folder is picked. */
        @Volatile
        var job: () -> BackupJob? = { null }

        /** Queues the daily work; one already queued is kept. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<BackupWorker>(24, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
