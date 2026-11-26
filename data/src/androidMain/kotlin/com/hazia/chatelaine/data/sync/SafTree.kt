package com.hazia.chatelaine.data.sync

import android.content.ContentResolver
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The few document operations [SafSyncFolder] is made of, on a tree the person picked through the Storage
 * Access Framework. Blocking: call it off the main thread.
 */
internal class SafTree(private val context: Context, val tree: Uri) {

    class Doc(val id: String, val name: String, val mime: String, val size: Long?, val flags: Int) {
        val isDir get() = mime == Document.MIME_TYPE_DIR
    }

    private val resolver: ContentResolver get() = context.contentResolver
    val rootId: String = DocumentsContract.getTreeDocumentId(tree)

    fun uri(id: String): Uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)

    /** The document [id] itself, or null when it is gone. */
    fun self(id: String): Doc? = try {
        resolver.query(uri(id), COLUMNS, null, null, null)?.use { c -> if (c.moveToFirst()) doc(c) else null }
    } catch (_: FileNotFoundException) {
        null
    } catch (_: IllegalArgumentException) {
        // How a provider in another process reports a missing document.
        null
    }

    fun children(parentId: String): List<Doc> =
        resolver.query(DocumentsContract.buildChildDocumentsUriUsingTree(tree, parentId), COLUMNS, null, null, null)?.use { c ->
            buildList { while (c.moveToNext()) add(doc(c)) }
        } ?: throw IOException("The folder could not be listed.")

    fun child(parentId: String, name: String): Doc? = children(parentId).firstOrNull { it.name == name }

    /** The directory at [segments] under the tree's root, made when [create]; null when it is missing and not made. */
    fun dir(segments: List<String>, create: Boolean): String? {
        var id = rootId
        for (segment in segments) {
            val found = child(id, segment)
            id = when {
                found == null && create -> create(id, segment, Document.MIME_TYPE_DIR).id
                found == null || !found.isDir -> return null
                else -> found.id
            }
        }
        return id
    }

    /** A new document named exactly [name] under [parentId]; the caller has made sure the name is free. */
    fun create(parentId: String, name: String, mime: String = OCTET): Doc {
        val made = DocumentsContract.createDocument(resolver, uri(parentId), mime, name) ?: throw IOException("Could not create $name.")
        val doc = self(DocumentsContract.getDocumentId(made)) ?: throw IOException("Could not create $name.")
        if (doc.name != name) {
            // A provider that renamed it (a name clash, an added extension) would break every later lookup.
            delete(doc.id)
            throw IOException("The folder named $name as ${doc.name}.")
        }
        return doc
    }

    fun delete(id: String) {
        if (!DocumentsContract.deleteDocument(resolver, uri(id))) throw IOException("Could not delete a file in the folder.")
    }

    /** Renames [id] to [name], which must be free, and returns its new id. */
    fun rename(id: String, name: String): String {
        val renamed = DocumentsContract.renameDocument(resolver, uri(id), name) ?: throw IOException("Could not rename to $name.")
        val newId = DocumentsContract.getDocumentId(renamed)
        if (self(newId)?.name != name) throw IOException("The folder did not rename the file to $name.")
        return newId
    }

    fun openIn(id: String): InputStream = resolver.openInputStream(uri(id)) ?: throw IOException("Could not open a file in the folder.")

    /** [mode] "w" for a new document, "wt" to rewrite one. */
    fun openOut(id: String, mode: String): OutputStream = resolver.openOutputStream(uri(id), mode) ?: throw IOException("Could not write a file in the folder.")

    /**
     * Whether the tree can be written now: the persisted read-and-write grant is there (a tree of the app's
     * own provider needs none), and the tree exists, is a directory, and takes new files. Never throws.
     */
    fun reachable(): Boolean = try {
        val granted = resolver.persistedUriPermissions.any { it.uri == tree && it.isReadPermission && it.isWritePermission } || ownProvider()
        val root = if (granted) self(rootId) else null
        root != null && root.isDir && root.flags and Document.FLAG_DIR_SUPPORTS_CREATE != 0
    } catch (_: SecurityException) {
        false
    } catch (_: IOException) {
        false
    } catch (_: IllegalArgumentException) {
        false
    } catch (_: IllegalStateException) {
        false
    }

    private fun ownProvider(): Boolean {
        val authority = tree.authority ?: return false
        val info = context.packageManager.resolveContentProvider(authority, PackageManager.ComponentInfoFlags.of(0))
        return info?.packageName == context.packageName
    }

    private fun doc(c: android.database.Cursor) = Doc(
        id = c.getString(0),
        name = c.getString(1),
        mime = c.getString(2) ?: OCTET,
        size = if (c.isNull(3)) null else c.getLong(3),
        flags = if (c.isNull(4)) 0 else c.getInt(4),
    )

    companion object {
        const val OCTET = "application/octet-stream"
        private val COLUMNS = arrayOf(
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE, Document.COLUMN_SIZE, Document.COLUMN_FLAGS,
        )
    }
}
