package com.hazia.chatelaine.data.blob

/** A device's stored photo files in memory, as a test's devices keep them. Like [DirectoryBlobStore], it refuses a name this app does not write. */
internal class MemoryBlobStore : BlobStore {
    val files = mutableMapOf<String, ByteArray>()

    override fun read(name: String): ByteArray? = files[check(name)]?.copyOf()
    override fun write(name: String, bytes: ByteArray) { files[check(name)] = bytes.copyOf() }
    override fun size(name: String): Long? = files[check(name)]?.size?.toLong()
    override fun delete(name: String) { files.remove(check(name)) }
    override fun list() = files.keys.toList()

    private fun check(name: String) = name.also { require(BlobName.isValid(it)) { "not a blob name: $it" } }
}
