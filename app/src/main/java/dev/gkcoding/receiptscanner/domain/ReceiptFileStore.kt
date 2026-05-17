package dev.gkcoding.receiptscanner.domain

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class StoredReceiptFile(
    val file: File,
    val mimeType: String,
)

class ReceiptFileStore(private val context: Context) {
    private val receiptDir: File
        get() = File(context.filesDir, "receipts").apply { mkdirs() }

    fun createCaptureFile(): File =
        File(receiptDir, "receipt_${System.currentTimeMillis()}.jpg")

    suspend fun copyImport(uri: Uri): StoredReceiptFile = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri) ?: "application/octet-stream"
        val extension = extensionFor(uri, mimeType)
        val destination = File(receiptDir, "receipt_${System.currentTimeMillis()}.$extension")

        resolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Unable to open selected file")

        StoredReceiptFile(destination, mimeType)
    }

    private fun extensionFor(uri: Uri, mimeType: String): String {
        val mimeExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (!mimeExtension.isNullOrBlank()) return mimeExtension

        val name = context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
        }

        return name?.substringAfterLast('.', missingDelimiterValue = "bin") ?: "bin"
    }
}
