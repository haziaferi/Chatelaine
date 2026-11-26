package com.hazia.chatelaine.data.sync

/**
 * What one folder line looks like on its way to the folder and back. The line holds no newline. It is a seam: the
 * folder is plain text now, and sealing the lines is a later change that touches only this interface.
 */
interface LineSeal {
    /** [b], one record's text, as the line to write (without its newline). */
    fun seal(b: ByteArray): String

    /** The record's bytes in [line], or null when the line cannot be opened. */
    fun open(line: String): ByteArray?
}

/** Lines are the records' UTF-8 text, as they are. */
object PlainLines : LineSeal {
    override fun seal(b: ByteArray): String = b.decodeToString()

    override fun open(line: String): ByteArray = line.encodeToByteArray()
}
