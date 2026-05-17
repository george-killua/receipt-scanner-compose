package dev.gkcoding.receiptscanner.data

import dev.gkcoding.receiptscanner.domain.ReceiptDraft

class ReceiptRepository(private val dao: ReceiptDao) {
    val receipts = dao.observeReceipts()

    suspend fun save(draft: ReceiptDraft): Long =
        dao.insert(
            ReceiptEntity(
                merchant = draft.merchant.ifBlank { "Unknown merchant" },
                amount = draft.amount.ifBlank { "Not found" },
                receiptDate = draft.receiptDate.ifBlank { "Not found" },
                originalFilePath = draft.originalFilePath,
                mimeType = draft.mimeType,
                ocrText = draft.ocrText,
            ),
        )

    suspend fun delete(id: Long) {
        dao.deleteById(id)
    }
}
