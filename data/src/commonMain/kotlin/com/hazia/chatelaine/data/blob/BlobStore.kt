package com.hazia.chatelaine.data.blob

import kotlin.jvm.JvmInline

/**
 * A photo file's bytes as this device keeps them, by blob name ([BlobName]); apart from the sync folder, which is only
 * how they travel. Blocking: call it off the main thread.
 */
interface BlobStore {
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray)
    /** The file's length in bytes, or null when it is not held. */
    fun size(name: String): Long?
    fun delete(name: String)
    fun list(): List<String>
}

/**
 * A stored file's name, on each device and in the sync folder: the SHA-256 of its bytes in lower-case hex (R56), so two
 * devices storing the same picture store one, and bytes that hash to something else are known for damaged.
 */
@JvmInline
value class BlobName(val value: String) {
    init {
        require(isValid(value)) { "not a blob name: $value" }
    }

    companion object {
        private val NAME = Regex("^[0-9a-f]{64}$")

        /** Whether [name] is one this app writes; anything else is left alone. */
        fun isValid(name: String) = NAME.matches(name)
    }
}
