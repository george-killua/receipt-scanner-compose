package dev.gkcoding.splitsnap.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.gkcoding.splitsnap.data.BillRepository
import dev.gkcoding.splitsnap.data.SplitSnapDatabase
import dev.gkcoding.splitsnap.domain.Bill
import dev.gkcoding.splitsnap.domain.BillItem
import dev.gkcoding.splitsnap.domain.OcrProcessor
import dev.gkcoding.splitsnap.domain.ReceiptFileStore
import dev.gkcoding.splitsnap.domain.ReceiptParser
import dev.gkcoding.splitsnap.domain.StoredReceiptFile
import java.io.File
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class Screen { Home, Camera, Editor, Summary }

data class SplitUiState(
    val bills: List<Bill> = emptyList(),
    val recentNames: List<String> = emptyList(),
    val screen: Screen = Screen.Home,
    val bill: Bill? = null,
    val selectedPersonKey: Long? = null,
    val isProcessing: Boolean = false,
    val notice: String? = null,
    val errorMessage: String? = null,
)

class SplitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = BillRepository(SplitSnapDatabase.get(application))
    private val fileStore = ReceiptFileStore(application)
    private val ocrProcessor = OcrProcessor(application)

    private var isSaving = false

    private val _uiState = MutableStateFlow(SplitUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.bills.collect { bills -> _uiState.update { it.copy(bills = bills) } }
        }
        viewModelScope.launch {
            repository.recentNames(exclude = ME).collect { names -> _uiState.update { it.copy(recentNames = names) } }
        }
    }

    override fun onCleared() {
        ocrProcessor.close()
    }

    // Navigation

    fun openCamera() {
        _uiState.update { it.copy(screen = Screen.Camera, errorMessage = null) }
    }

    fun closeCamera() {
        _uiState.update { it.copy(screen = Screen.Home) }
    }

    fun startManualBill() {
        openEditor(newBill(), notice = null)
    }

    fun openBill(bill: Bill) {
        _uiState.update { it.copy(bill = bill, screen = Screen.Summary, selectedPersonKey = null, notice = null) }
    }

    fun editBill() {
        _uiState.update { it.copy(screen = Screen.Editor) }
    }

    fun showSummary() {
        persist { _uiState.update { it.copy(screen = Screen.Summary, selectedPersonKey = null, notice = null) } }
    }

    fun goHome() {
        val bill = uiState.value.bill
        val keep = bill != null && (bill.items.isNotEmpty() || bill.id != 0L)
        if (keep) {
            persist { _uiState.update { it.copy(screen = Screen.Home, bill = null) } }
        } else {
            _uiState.update { it.copy(screen = Screen.Home, bill = null) }
        }
    }

    // Capture and import

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
                    _uiState.update { it.copy(isProcessing = false, errorMessage = throwable.message ?: "Import failed") }
                }
        }
    }

    private fun processStoredFile(file: StoredReceiptFile) {
        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, screen = Screen.Home, errorMessage = null) }

            runCatching {
                val text = ocrProcessor.recognize(file)
                val receipt = ReceiptParser.parse(text)
                newBill().copy(
                    merchant = receipt.merchant,
                    date = receipt.date,
                    currencySymbol = receipt.currencySymbol ?: defaultCurrencySymbol(),
                    items = receipt.items.mapIndexed { index, item -> BillItem(100L + index, item.name, item.priceCents) },
                    taxCents = receipt.taxCents,
                    tipCents = receipt.tipCents,
                    receiptTotalCents = receipt.totalCents,
                    originalFilePath = file.file.absolutePath,
                    mimeType = file.mimeType,
                    ocrText = text,
                )
            }.onSuccess { bill ->
                _uiState.update { it.copy(isProcessing = false) }
                openEditor(
                    bill,
                    notice = if (bill.items.isEmpty()) "No line items found. Add them below, or try a sharper, flatter photo." else null,
                )
            }.onFailure { throwable ->
                fileStore.delete(file.file.absolutePath)
                _uiState.update { it.copy(isProcessing = false, errorMessage = throwable.message ?: "Couldn't read that receipt") }
            }
        }
    }

    // Editing

    fun selectPerson(key: Long?) {
        _uiState.update { it.copy(selectedPersonKey = if (it.selectedPersonKey == key) null else key) }
    }

    fun addPerson(name: String) {
        if (name.isBlank()) return
        editBill { bill ->
            val updated = bill.withPerson(name)
            _uiState.update { it.copy(selectedPersonKey = updated.people.last().key) }
            updated
        }
    }

    fun removePerson(key: Long) {
        editBill { it.withoutPerson(key) }
        _uiState.update { if (it.selectedPersonKey == key) it.copy(selectedPersonKey = null) else it }
    }

    fun renamePerson(key: Long, name: String) {
        if (name.isBlank()) return
        editBill { bill -> bill.copy(people = bill.people.map { if (it.key == key) it.copy(name = name.trim()) else it }) }
    }

    fun onItemTapped(itemKey: Long) {
        val personKey = uiState.value.selectedPersonKey ?: return
        editBill { it.toggleAssignment(itemKey, personKey) }
    }

    fun saveItem(item: BillItem) {
        editBill { bill ->
            if (bill.items.any { it.key == item.key }) {
                bill.copy(items = bill.items.map { if (it.key == item.key) item else it })
            } else {
                bill.copy(items = bill.items + item.copy(key = bill.nextKey()))
            }
        }
    }

    fun deleteItem(key: Long) {
        editBill { bill -> bill.copy(items = bill.items.filterNot { it.key == key }) }
    }

    fun splitRestEvenly() {
        editBill { it.splitUnassignedEvenly() }
    }

    fun updateDetails(merchant: String, date: String, currencySymbol: String) {
        editBill { it.copy(merchant = merchant.trim(), date = date.trim(), currencySymbol = currencySymbol.trim().ifEmpty { it.currencySymbol }) }
    }

    fun setTax(cents: Long) {
        editBill { it.copy(taxCents = cents) }
    }

    fun setTip(cents: Long) {
        editBill { it.copy(tipCents = cents) }
    }

    fun dismissNotice() {
        _uiState.update { it.copy(notice = null) }
    }

    // Summary

    fun togglePaid(personKey: Long) {
        val bill = uiState.value.bill ?: return
        val person = bill.people.firstOrNull { it.key == personKey } ?: return
        val paid = !person.paid
        _uiState.update { state ->
            state.copy(bill = bill.copy(people = bill.people.map { if (it.key == personKey) it.copy(paid = paid) else it }))
        }
        viewModelScope.launch { repository.setPaid(personKey, paid) }
    }

    fun deleteBill() {
        val bill = uiState.value.bill ?: return
        _uiState.update { it.copy(bill = null, screen = Screen.Home) }
        viewModelScope.launch {
            if (bill.id != 0L) repository.delete(bill.id)
            fileStore.delete(bill.originalFilePath)
        }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun openEditor(bill: Bill, notice: String?) {
        _uiState.update {
            it.copy(bill = bill, screen = Screen.Editor, selectedPersonKey = null, notice = notice)
        }
    }

    private inline fun editBill(transform: (Bill) -> Bill) {
        val bill = uiState.value.bill ?: return
        val updated = transform(bill)
        _uiState.update { it.copy(bill = updated) }
    }

    /** Saves the current bill and swaps in the stored copy, whose keys are database ids. */
    private fun persist(onSaved: () -> Unit) {
        val bill = uiState.value.bill ?: return onSaved()
        if (isSaving) return
        isSaving = true
        viewModelScope.launch {
            runCatching { repository.save(bill) }
                .onSuccess { saved ->
                    _uiState.update { state -> if (state.bill == bill) state.copy(bill = saved) else state }
                    onSaved()
                }
                .onFailure { throwable -> showError(throwable.message ?: "Couldn't save the bill") }
            isSaving = false
        }
    }

    private fun newBill() = Bill(currencySymbol = defaultCurrencySymbol()).withPerson(ME)

    private fun defaultCurrencySymbol(): String =
        runCatching { Currency.getInstance(Locale.getDefault()).symbol }.getOrNull()
            ?.takeIf { it.length <= 2 }
            ?: "€"

    companion object {
        const val ME = "Me"
    }
}
