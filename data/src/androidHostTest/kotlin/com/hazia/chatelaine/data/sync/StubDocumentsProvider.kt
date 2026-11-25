package com.hazia.chatelaine.data.sync

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.File
import java.io.FileNotFoundException

/**
 * A documents provider over a plain directory for the host tests of [SafSyncFolder]. Document ids are paths
 * under [base]. [onCall] sees every call before it runs, so a test can look at the folder in the middle of a write.
 *
 * A plain [ContentProvider] that answers the calls of `DocumentsContract`, not a `DocumentsProvider`: Robolectric
 * sends queries in the form that class refuses.
 */
class StubDocumentsProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor {
        val id = DocumentsContract.getDocumentId(uri)
        val cursor = MatrixCursor(projection ?: COLUMNS)
        if (uri.pathSegments.lastOrNull() == "children") {
            onCall("children $id")
            file(id).listFiles()?.sortedBy { it.name }?.forEach { row(cursor, "$id/${it.name}", it) }
        } else {
            onCall("query $id")
            val file = file(id)
            if (!file.exists()) throw FileNotFoundException(id)
            row(cursor, id, file)
        }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        val id = DocumentsContract.getDocumentId(uri)
        onCall("open $id $mode")
        return ParcelFileDescriptor.open(file(id), ParcelFileDescriptor.parseMode(mode))
    }

    override fun call(method: String, arg: String?, extras: Bundle?): Bundle? {
        val uri = extras?.getParcelable(EXTRA_URI, Uri::class.java) ?: return null
        val id = DocumentsContract.getDocumentId(uri)
        val result = Bundle()
        when (method) {
            "android:createDocument" -> {
                val name = extras.getString(Document.COLUMN_DISPLAY_NAME)!!
                onCall("create $id/$name")
                val file = File(file(id), name)
                val made = if (extras.getString(Document.COLUMN_MIME_TYPE) == Document.MIME_TYPE_DIR) file.mkdir() else file.createNewFile()
                check(made) { "could not create $name" }
                result.putParcelable(EXTRA_URI, DocumentsContract.buildDocumentUriUsingTree(uri, "$id/$name"))
            }
            "android:deleteDocument" -> {
                onCall("delete $id")
                check(file(id).deleteRecursively()) { "could not delete $id" }
            }
            "android:renameDocument" -> {
                val name = extras.getString(Document.COLUMN_DISPLAY_NAME)!!
                onCall("rename $id $name")
                val from = file(id)
                val to = File(from.parentFile, name)
                check(!to.exists() && from.renameTo(to)) { "could not rename $id" }
                result.putParcelable(EXTRA_URI, DocumentsContract.buildDocumentUriUsingTree(uri, id.substringBeforeLast('/') + "/" + name))
            }
            else -> return null
        }
        return result
    }

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = 0

    private fun file(documentId: String): File {
        require(documentId.split('/').none { it == ".." || it.isEmpty() }) { "bad id: $documentId" }
        return File(base, documentId)
    }

    private fun row(cursor: MatrixCursor, id: String, file: File) {
        val dir = file.isDirectory
        cursor.newRow()
            .add(Document.COLUMN_DOCUMENT_ID, id)
            .add(Document.COLUMN_DISPLAY_NAME, file.name)
            .add(Document.COLUMN_MIME_TYPE, if (dir) Document.MIME_TYPE_DIR else "application/octet-stream")
            .add(Document.COLUMN_SIZE, if (dir) null else file.length())
            .add(Document.COLUMN_LAST_MODIFIED, file.lastModified())
            .add(
                Document.COLUMN_FLAGS,
                Document.FLAG_SUPPORTS_DELETE or Document.FLAG_SUPPORTS_RENAME or
                    if (dir) Document.FLAG_DIR_SUPPORTS_CREATE else Document.FLAG_SUPPORTS_WRITE,
            )
    }

    companion object {
        private const val EXTRA_URI = "uri" // the hidden DocumentsContract.EXTRA_URI
        const val AUTHORITY = "com.hazia.chatelaine.test.docs"

        /** The directory the document ids are paths under; a test sets it before it creates the provider. */
        @Volatile var base: File = File(".")

        @Volatile var onCall: (String) -> Unit = {}

        private val COLUMNS = arrayOf(
            Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE, Document.COLUMN_LAST_MODIFIED, Document.COLUMN_FLAGS,
        )
    }
}
