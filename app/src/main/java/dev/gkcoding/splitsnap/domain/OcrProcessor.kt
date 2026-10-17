package dev.gkcoding.splitsnap.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.core.net.toUri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class OcrProcessor(private val context: Context) {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Returns the receipt text with one visual row per line. */
    suspend fun recognize(file: StoredReceiptFile): String {
        val result = if (file.mimeType == "application/pdf" || file.file.extension.equals("pdf", ignoreCase = true)) {
            recognizePdfFirstPage(file.file)
        } else {
            recognizeImage(file.file)
        } ?: return ""

        return ReceiptRows.build(result.toOcrLines()).joinToString("\n")
    }

    fun close() {
        recognizer.close()
    }

    private suspend fun recognizeImage(file: File): Text {
        val image = InputImage.fromFilePath(context, file.toUri())
        return recognizer.process(image).await()
    }

    private suspend fun recognizePdfFirstPage(file: File): Text? = withContext(Dispatchers.IO) {
        ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
            PdfRenderer(descriptor).use { renderer ->
                if (renderer.pageCount == 0) return@withContext null

                renderer.openPage(0).use { page ->
                    val scale = 2
                    val bitmap = Bitmap.createBitmap(
                        page.width * scale,
                        page.height * scale,
                        Bitmap.Config.ARGB_8888,
                    )
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    try {
                        recognizer.process(InputImage.fromBitmap(bitmap, 0)).await()
                    } finally {
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    private fun Text.toOcrLines(): List<OcrLine> =
        textBlocks.flatMap { it.lines }.mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            OcrLine(line.text, box.left, box.top, box.right, box.bottom)
        }
}
