package com.hazia.chatelaine.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** [BackupArchive] on `java.util.zip`. Entry times are fixed, so the same entries give the same bytes. */
object ZipBackup : BackupArchive {
    private const val FIXED_TIME = 315_532_800_000L // 1980-01-01, the earliest a zip can hold

    override fun pack(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, bytes) in entries) {
                zip.putNextEntry(ZipEntry(name).apply { time = FIXED_TIME })
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    override fun unpack(zip: ByteArray): Map<String, ByteArray> {
        val entries = LinkedHashMap<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(zip)).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (!entry.isDirectory) entries[entry.name] = input.readBytes()
                }
            }
        } catch (_: IOException) {
            // A damaged zip yields what was read before the damage; the caller finds the entry it needs missing.
        }
        return entries
    }
}
