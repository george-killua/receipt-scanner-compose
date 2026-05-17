package dev.gkcoding.receiptscanner.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.gkcoding.receiptscanner.data.ReceiptEntity
import dev.gkcoding.receiptscanner.domain.ReceiptDraft
import java.io.File

@Composable
fun ReceiptScannerApp(viewModel: ReceiptViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = androidx.compose.ui.graphics.Color(0xFF0F766E),
            secondary = androidx.compose.ui.graphics.Color(0xFF475569),
            surface = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
            background = androidx.compose.ui.graphics.Color(0xFFF8FAFC),
        ),
    ) {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (state.cameraOpen) {
                CameraCaptureScreen(
                    createFile = viewModel::createCaptureFile,
                    onClose = viewModel::closeCamera,
                    onCaptured = viewModel::processCapturedFile,
                    onError = { message ->
                        viewModel.closeCamera()
                        viewModel.showError(message)
                    },
                )
            } else {
                ReceiptHomeScreen(
                    state = state,
                    onOpenCamera = viewModel::openCamera,
                    onImport = viewModel::processImport,
                    onSelectReceipt = viewModel::selectReceipt,
                    onDeleteReceipt = viewModel::deleteReceipt,
                    onDismissReceipt = { viewModel.selectReceipt(null) },
                    onDraftChange = viewModel::updateDraft,
                    onSaveDraft = viewModel::saveDraft,
                    onDismissDraft = viewModel::dismissDraft,
                    onDismissError = viewModel::clearError,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReceiptHomeScreen(
    state: ReceiptUiState,
    onOpenCamera: () -> Unit,
    onImport: (android.net.Uri) -> Unit,
    onSelectReceipt: (ReceiptEntity) -> Unit,
    onDeleteReceipt: (Long) -> Unit,
    onDismissReceipt: () -> Unit,
    onDraftChange: (ReceiptDraft) -> Unit,
    onSaveDraft: () -> Unit,
    onDismissDraft: () -> Unit,
    onDismissError: () -> Unit,
) {
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImport)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) onOpenCamera()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Receipt Scanner") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.CAMERA,
                        ) == PackageManager.PERMISSION_GRANTED

                        if (granted) onOpenCamera() else permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                ) {
                    Text("Scan")
                }
                OutlinedButton(onClick = { picker.launch(arrayOf("image/*", "application/pdf")) }) {
                    Text("Import")
                }
            }

            if (state.isProcessing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                    Text("Reading receipt...")
                }
            }

            state.errorMessage?.let { message ->
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(message, modifier = Modifier.weight(1f))
                        TextButton(onClick = onDismissError) { Text("Dismiss") }
                    }
                }
            }

            Text(
                text = "Saved receipts",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.receipts.isEmpty()) {
                EmptyState()
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(state.receipts, key = { it.id }) { receipt ->
                        ReceiptRow(receipt = receipt, onClick = { onSelectReceipt(receipt) })
                    }
                }
            }
        }
    }

    state.draft?.let { draft ->
        ReviewDialog(
            draft = draft,
            onDraftChange = onDraftChange,
            onSave = onSaveDraft,
            onDismiss = onDismissDraft,
        )
    }

    state.selectedReceipt?.let { receipt ->
        ReceiptDetailDialog(
            receipt = receipt,
            onDelete = { onDeleteReceipt(receipt.id) },
            onDismiss = onDismissReceipt,
        )
    }
}

@Composable
private fun ReceiptRow(receipt: ReceiptEntity, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = receipt.merchant,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(receipt.amount, fontWeight = FontWeight.SemiBold)
            }
            Text(receipt.receiptDate, color = MaterialTheme.colorScheme.secondary)
            Text(File(receipt.originalFilePath).name, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun EmptyState() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("No receipts yet", style = MaterialTheme.typography.titleMedium)
            Text("Scan a receipt or import an image/PDF to review and save extracted fields.")
        }
    }
}

@Composable
private fun ReviewDialog(
    draft: ReceiptDraft,
    onDraftChange: (ReceiptDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Review receipt") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = draft.merchant,
                    onValueChange = { onDraftChange(draft.copy(merchant = it)) },
                    label = { Text("Merchant") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.amount,
                    onValueChange = { onDraftChange(draft.copy(amount = it)) },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.receiptDate,
                    onValueChange = { onDraftChange(draft.copy(receiptDate = it)) },
                    label = { Text("Date") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text("OCR text", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = draft.ocrText.ifBlank { "No text found" },
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        confirmButton = {
            Button(onClick = onSave) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun ReceiptDetailDialog(
    receipt: ReceiptEntity,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(receipt.merchant) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("Amount: ${receipt.amount}")
                Text("Date: ${receipt.receiptDate}")
                Text("File: ${receipt.originalFilePath}")
                Spacer(modifier = Modifier.height(8.dp))
                Text(receipt.ocrText.ifBlank { "No OCR text stored" })
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onDelete) { Text("Delete") }
        },
    )
}
