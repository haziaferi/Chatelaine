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
import com.hazia.chatelaine.data.ChatelaineData
import com.hazia.chatelaine.data.DEVICE_ID_DIR
import com.hazia.chatelaine.data.HarnessItem
import com.hazia.chatelaine.data.blob.DirectoryBlobStore
import com.hazia.chatelaine.data.deviceId
import com.hazia.chatelaine.data.openChatelaineDatabase
import com.hazia.chatelaine.data.sync.SafSyncFolder
import com.hazia.chatelaine.data.sync.SyncFolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Stage 1 debug harness (provisional, not a product screen, debug builds only: it lives in the `debug` source set, so
 * a release APK holds none of it). Plain controls for the five actions: add, edit (tap an item to pick it, then type
 * the new name), list, sync and backup. The sync folder is picked once with the system folder picker; the permission
 * is persisted and the folder is used through [SafSyncFolder]. The database and `identity/device-id` are in `filesDir`.
 *
 * Install: `./gradlew :androidApp:installDebug` (or `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`).
 */
class HarnessActivity : ComponentActivity() {

    private val data: ChatelaineData by lazy {
        ChatelaineData(
            open = openChatelaineDatabase(this),
            deviceId = deviceId(File(filesDir, "$DEVICE_ID_DIR/device-id")),
            photos = DirectoryBlobStore(File(filesDir, "photos").toPath()),
        )
    }

    private val prefs get() = getSharedPreferences("harness", MODE_PRIVATE)

    private fun savedFolder(): Uri? = prefs.getString(FOLDER, null)?.let(Uri::parse)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { Harness() }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isFinishing) runCatching { data.close() }
    }

    @Composable
    private fun Harness() {
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var picked by remember { mutableStateOf<String?>(null) }
        var folder by remember { mutableStateOf(savedFolder()) }
        var items by remember { mutableStateOf(emptyList<HarnessItem>()) }
        var message by remember { mutableStateOf("") }

        suspend fun refresh() { items = withContext(Dispatchers.IO) { data.list() } }

        fun act(what: String, block: suspend () -> String) {
            scope.launch {
                message = try { withContext(Dispatchers.IO) { block() } } catch (e: Exception) { "$what failed: $e" }
                refresh()
            }
        }

        val pick = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            if (uri != null) {
                contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                prefs.edit().putString(FOLDER, uri.toString()).apply()
                folder = uri
            }
        }

        // One object per picked folder: the sync keeps what it read with it.
        val saf = remember(folder) { folder?.let { SafSyncFolder(this, it) } }
        fun syncFolder(): SyncFolder = requireNotNull(saf) { "pick the sync folder first" }

        LaunchedEffect(Unit) { refresh() }

        Column(Modifier.padding(16.dp)) {
            BasicText("device ${data.deviceId}")
            BasicText("sync folder: ${folder ?: "none picked"}")
            BasicText(message)
            Button("Pick sync folder") { pick.launch(null) }
            BasicTextField(name, { name = it }, Modifier.border(1.dp, Color.Gray).padding(8.dp))
            Row {
                Button("Add") { act("add") { "added " + data.add(name).also { name = "" } } }
                Button("Edit picked") { act("edit") { data.edit(requireNotNull(picked) { "tap an item first" }, name); name = ""; "edited" } }
            }
            Row {
                Button("List") { act("list") { "listed" } }
                Button("Sync") { act("sync") { "synced in ${data.sync(syncFolder())} round(s)" } }
                Button("Backup") { act("backup") { data.backup(syncFolder()) ?: "no backup now: re-reading its own log" } }
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
    }
}
