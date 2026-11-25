package com.hazia.chatelaine.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * [BackupArchive] on `java.util.zip`. Entry times are fixed, so the same entries give the same bytes. The records are
 * deflated; `blobs/` entries are stored as they are, since photo files are already compressed.
 */
object ZipBackup : BackupArchive {
    private const val FIXED_TIME = 315_532_800_000L // 1980-01-01, the earliest a zip can hold
    private const val BLOBS = "blobs/"

    override fun pack(entries: Map<String, ByteArray>): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            for ((name, bytes) in entries) {
                val entry = ZipEntry(name).apply { time = FIXED_TIME }
                if (name.startsWith(BLOBS)) {
                    entry.method = ZipEntry.STORED
                    entry.size = bytes.size.toLong()
                    entry.compressedSize = bytes.size.toLong()
                    entry.crc = CRC32().apply { update(bytes) }.value
                }
                zip.putNextEntry(entry)
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /**
     * The entries of [zip]; empty when it is damaged or cut short: any [IOException] while reading, or no end-of-
     * directory record at its end (a zip cut between two entries still reads, entry by entry, but is not whole).
     */
    override fun unpack(zip: ByteArray): Map<String, ByteArray> {
        if (!hasEndRecord(zip)) return emptyMap()
        val entries = LinkedHashMap<String, ByteArray>()
        try {
            ZipInputStream(ByteArrayInputStream(zip)).use { input ->
                while (true) {
                    val entry = input.nextEntry ?: break
                    if (!entry.isDirectory) entries[entry.name] = input.readBytes()
                }
            }
        } catch (_: IOException) {
            return emptyMap()
        }
        return entries
    }

    /** The "end of central directory" signature (PK 05 06) in the last 22 bytes plus a comment of up to 64 KiB. */
    private fun hasEndRecord(zip: ByteArray): Boolean {
        val first = maxOf(0, zip.size - 22 - 65_535)
        for (i in zip.size - 22 downTo first) {
            if (zip[i] == 0x50.toByte() && zip[i + 1] == 0x4b.toByte() && zip[i + 2] == 0x05.toByte() && zip[i + 3] == 0x06.toByte()) return true
        }
        return false
    }
}
