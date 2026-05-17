package dev.gkcoding.receiptscanner.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.gkcoding.receiptscanner.data.ReceiptDatabase
import dev.gkcoding.receiptscanner.data.ReceiptEntity
import dev.gkcoding.receiptscanner.data.ReceiptRepository
import dev.gkcoding.receiptscanner.domain.OcrProcessor
import dev.gkcoding.receiptscanner.domain.ReceiptDraft
import dev.gkcoding.receiptscanner.domain.ReceiptFileStore
import dev.gkcoding.receiptscanner.domain.ReceiptParser
import dev.gkcoding.receiptscanner.domain.StoredReceiptFile
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReceiptUiState(
    val receipts: List<ReceiptEntity> = emptyList(),
    val draft: ReceiptDraft? = null,
    val selectedReceipt: ReceiptEntity? = null,
    val cameraOpen: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,
)

class ReceiptViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReceiptRepository(
        ReceiptDatabase.get(application).receiptDao(),
    )
    private val fileStore = ReceiptFileStore(application)
    private val ocrProcessor = OcrProcessor(application)

    private val _uiState = MutableStateFlow(ReceiptUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.receipts.collect { receipts ->
                _uiState.update { it.copy(receipts = receipts) }
            }
        }
    }

    fun openCamera() {
        _uiState.update { it.copy(cameraOpen = true, errorMessage = null) }
    }

    fun closeCamera() {
        _uiState.update { it.copy(cameraOpen = false) }
    }

    fun createCaptureFile(): File = fileStore.createCaptureFile()

    fun processCapturedFile(file: File) {
        processStoredFile(StoredReceiptFile(file, "image/jpeg"))
    }

    fun processImport(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }
            runCatching { fileStore.copyImport(uri) }
                .onSuccess(::processStoredFile)
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = throwable.message ?: "Import failed",
                        )
                    }
                }
        }
    }

    fun updateDraft(draft: ReceiptDraft) {
        _uiState.update { it.copy(draft = draft) }
    }

    fun dismissDraft() {
        _uiState.update { it.copy(draft = null) }
    }

    fun saveDraft() {
        val draft = uiState.value.draft ?: return
        viewModelScope.launch {
            repository.save(draft)
            _uiState.update { it.copy(draft = null) }
        }
    }

    fun selectReceipt(receipt: ReceiptEntity?) {
        _uiState.update { it.copy(selectedReceipt = receipt) }
    }

    fun deleteReceipt(id: Long) {
        viewModelScope.launch {
            repository.delete(id)
            _uiState.update { it.copy(selectedReceipt = null) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    private fun processStoredFile(file: StoredReceiptFile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, cameraOpen = false, errorMessage = null) }

            runCatching {
                val text = ocrProcessor.recognize(file)
                ReceiptParser.parse(
                    text = text,
                    originalFilePath = file.file.absolutePath,
                    mimeType = file.mimeType,
                )
            }.onSuccess { draft ->
                _uiState.update { it.copy(isProcessing = false, draft = draft) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isProcessing = false,
                        errorMessage = throwable.message ?: "OCR failed",
                    )
                }
            }
        }
    }
}
