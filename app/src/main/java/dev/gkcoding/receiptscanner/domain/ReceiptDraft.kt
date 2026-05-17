package dev.gkcoding.receiptscanner.domain

data class ReceiptDraft(
    val merchant: String = "",
    val amount: String = "",
    val receiptDate: String = "",
    val originalFilePath: String = "",
    val mimeType: String = "",
    val ocrText: String = "",
)
