package dev.gkcoding.receiptscanner.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val merchant: String,
    val amount: String,
    val receiptDate: String,
    val originalFilePath: String,
    val mimeType: String,
    val ocrText: String,
    val createdAt: Long = System.currentTimeMillis(),
)
