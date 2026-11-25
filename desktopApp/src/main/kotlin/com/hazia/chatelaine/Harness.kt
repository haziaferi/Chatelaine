package com.hazia.chatelaine

import com.hazia.chatelaine.data.ChatelaineData
import com.hazia.chatelaine.data.DATABASE_NAME
import com.hazia.chatelaine.data.DEVICE_ID_DIR
import com.hazia.chatelaine.data.blob.DirectoryBlobStore
import com.hazia.chatelaine.data.deviceId
import com.hazia.chatelaine.data.openChatelaineDatabase
import com.hazia.chatelaine.data.sync.DirectorySyncFolder
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.system.exitProcess

/**
 * Stage 1 debug harness (provisional, not a product screen; stage 2 replaces it). Run from the repository root:
 *
 *     ./gradlew :desktopApp:harness --args="--data build/harness-data add hammer"
 *
 * Actions: `add <name>`, `edit <id> <name>`, `list` (id, name, and the names later edits overwrote, from History),
 * `sync <folder>` (export, import and photo exchange until nothing changes), `backup <folder>`.
 * `--data <dir>` (anywhere in the arguments; default `build/harness-data`) holds the database, the photo files and
 * `identity/device-id`. Two data folders on one PC make two devices.
 */
fun main(args: Array<String>) {
    val rest = args.toMutableList()
    val flag = rest.indexOf("--data")
    var dataDir = File("build/harness-data")
    if (flag >= 0) {
        if (flag + 1 >= rest.size) usage("--data needs a folder")
        dataDir = File(rest[flag + 1])
        rest.subList(flag, flag + 2).clear()
    }
    val action = rest.firstOrNull() ?: usage("no action")
    val operands = rest.drop(1)

    val data = ChatelaineData(
        open = openChatelaineDatabase(File(dataDir, DATABASE_NAME)),
        deviceId = deviceId(File(dataDir, "$DEVICE_ID_DIR/device-id")),
        photos = DirectoryBlobStore(File(dataDir, "photos").toPath()),
    )
    data.use {
        runBlocking {
            when (action) {
                "add" -> println(it.add(operands.singleOrNull() ?: usage("add <name>")))
                "edit" -> {
                    if (operands.size != 2) usage("edit <id> <name>")
                    it.edit(operands[0], operands[1])
                    println("ok")
                }
                "list" -> it.list().forEach { item ->
                    println("${item.id}  ${item.name}" + if (item.lost.isEmpty()) "" else "  (History: ${item.lost.joinToString(", ")})")
                }
                "sync" -> println("synced in ${it.sync(DirectorySyncFolder(folderArg(operands).toPath()))} round(s)")
                "backup" -> println(it.backup(DirectorySyncFolder(folderArg(operands).toPath())) ?: "no backup now: the database is re-reading its own log")
                else -> usage("unknown action $action")
            }
        }
    }
}

private fun folderArg(operands: List<String>): File = File(operands.singleOrNull() ?: usage("a folder is needed")).also { it.mkdirs() }

private fun usage(why: String): Nothing {
    System.err.println("$why\nusage: [--data <dir>] add <name> | edit <id> <name> | list | sync <folder> | backup <folder>")
    exitProcess(2)
}
