package com.hazia.chatelaine

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.hazia.chatelaine.data.DATABASE_NAME
import com.hazia.chatelaine.data.ITEM
import com.hazia.chatelaine.data.app.LocalAppData
import com.hazia.chatelaine.data.openChatelaineDatabase
import com.hazia.chatelaine.data.sync.SafSyncFolder
import com.hazia.chatelaine.data.sync.SyncFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** An item as the harness lists it: its names that a later edit overwrote ([lost]) are what History holds. */
private data class HarnessItem(val id: String, val name: String, val lost: List<String>)

/**
 * Stage 1 debug harness (provisional, not a product screen, debug builds only: it lives in the `debug` source set, so
 * a release APK holds none of it). Plain controls for the five actions: add, edit (tap an item to pick it, then type
 * the new name), list, sync and backup. The sync folder and the backup folder, two different folders, are each picked
 * once with the system folder picker; the permissions are persisted and the folders are used through [SafSyncFolder].
 * The database and `identity/device-id` are in `filesDir`. It reaches them through [LocalAppData.shared], the
 * process's one opener, so the harness and the app never open the database twice; the harness never closes it.
 *
 * Install: `./gradlew :androidApp:installDebug` (or `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`).
 */
class HarnessActivity : ComponentActivity() {

    private val data: LocalAppData by lazy {
        LocalAppData.shared(applicationContext.getDatabasePath(DATABASE_NAME), filesDir, open = { openChatelaineDatabase(this) })
    }

    private suspend fun list(): List<HarnessItem> =
        data.inventory.items().sortedBy { it.name }.map { item ->
            HarnessItem(item.id, item.name, data.history.overwritten(ITEM, item.id, NAME_GROUP).mapNotNull { it.values["name"] as? String })
        }

    private val prefs get() = getSharedPreferences("harness", MODE_PRIVATE)

    private fun savedFolder(key: String): Uri? = prefs.getString(key, null)?.let(Uri::parse)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Harness() }
    }

    @Composable
    private fun Harness() {
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var picked by remember { mutableStateOf<String?>(null) }
        var folder by remember { mutableStateOf(savedFolder(FOLDER)) }
        var backupFolder by remember { mutableStateOf(savedFolder(BACKUP_FOLDER)) }
        var items by remember { mutableStateOf(emptyList<HarnessItem>()) }
        var message by remember { mutableStateOf("") }

        suspend fun refresh() { items = withContext(Dispatchers.IO) { list() } }

        fun act(what: String, block: suspend () -> String) {
            scope.launch {
                message = try { withContext(Dispatchers.IO) { block() } } catch (e: Exception) { "$what failed: $e" }
                refresh()
            }
        }

        fun keep(uri: Uri, key: String) {
            contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
            prefs.edit().putString(key, uri.toString()).apply()
        }
        val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) { keep(uri, FOLDER); folder = uri }
        }
        val pickBackup = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) { keep(uri, BACKUP_FOLDER); backupFolder = uri }
        }

        // One object per picked folder: the sync keeps what it read with it.
        val saf = remember(folder) { folder?.let { SafSyncFolder(this, it) } }
        fun syncFolder(): SyncFolder = requireNotNull(saf) { "pick the sync folder first" }
        // Backups go to a folder of their own: in the sync folder, every device would carry every backup zip.
        fun backupTarget(): SyncFolder =
            SafSyncFolder(this, Uri.parse(backupFolderOf(backupFolder?.toString(), folder?.toString())))

        LaunchedEffect(Unit) { refresh() }

        Column(Modifier.padding(16.dp)) {
            BasicText("device ${data.deviceId}")
            BasicText("sync folder: ${folder ?: "none picked"}")
            BasicText("backup folder: ${backupFolder ?: "none picked"}")
            BasicText(message)
            Row {
                Button("Pick sync folder") { pick.launch(null) }
                Button("Pick backup folder") { pickBackup.launch(null) }
            }
            BasicTextField(name, { name = it }, Modifier.border(1.dp, Color.Gray).padding(8.dp))
            Row {
                Button("Add") { act("add") { "added " + data.inventory.addItem(name).also { name = "" } } }
                Button("Edit picked") { act("edit") { data.inventory.rename(requireNotNull(picked) { "tap an item first" }, name); name = ""; "edited" } }
            }
            Row {
                Button("List") { act("list") { "listed" } }
                Button("Sync") { act("sync") { "synced in ${data.sync(syncFolder())} round(s)" } }
                Button("Backup") { act("backup") { data.backup(backupTarget(), System.currentTimeMillis()) ?: "no backup now: re-reading its own log" } }
            }
            LazyColumn {
                items(items) { item ->
                    val mark = if (item.id == picked) "> " else ""
                    BasicText(
                        "$mark${item.name}  ${item.id.take(8)}" + if (item.lost.isEmpty()) "" else "  (History: ${item.lost.joinToString(", ")})",
                        Modifier.clickable { picked = item.id }.padding(4.dp),
                    )
                }
            }
        }
    }

    @Composable
    private fun Button(label: String, onClick: () -> Unit) =
        BasicText("[ $label ]", Modifier.clickable(onClick = onClick).padding(8.dp))

    private companion object {
        const val FOLDER = "folder"
        const val BACKUP_FOLDER = "backupFolder"

        /** The field group of an item's name, as History keys it. */
        const val NAME_GROUP = "name"
    }
}

/**
 * The folder the harness's Backup button writes to, as a tree URI: its own pick, never the sync folder ([sync]), where
 * every backup zip would travel to every device.
 */
internal fun backupFolderOf(backup: String?, sync: String?): String {
    requireNotNull(backup) { "pick the backup folder first" }
    require(backup != sync) { "the backup folder must be another folder than the sync folder" }
    return backup
}
