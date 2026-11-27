package com.hazia.chatelaine.data.app

import com.hazia.chatelaine.data.backup.BackupSchedule
import com.hazia.chatelaine.data.sync.SyncFolder

/**
 * The daily backup check (R60): [AppData.backup] into the backup folder
 * of [settings], opened through [folderOf], when one is set and a day has passed since the last backup
 * ([BackupSchedule.due]), or whenever [force]. Only a backup made records [now] as the last one, so a device that was
 * off for days makes one at start, and one held after a recovery is tried again next time. Returns its name, or null
 * when none was made.
 */
suspend fun backupIfDue(app: AppData, settings: SettingsStore, folderOf: (String) -> SyncFolder, now: Long, force: Boolean = false): String? {
    val current = settings.settings.value
    val folder = current.backupFolder ?: return null
    if (!force && !BackupSchedule.due(current.lastBackupMillis, now)) return null
    val name = app.backup(folderOf(folder), now) ?: return null
    settings.update { it.copy(lastBackupMillis = now) }
    return name
}
