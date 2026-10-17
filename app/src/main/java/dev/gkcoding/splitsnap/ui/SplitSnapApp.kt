package dev.gkcoding.splitsnap.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun SplitSnapApp(viewModel: SplitViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    SplitSnapTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val bill = state.bill
            when {
                state.screen == Screen.Camera -> {
                    BackHandler(onBack = viewModel::closeCamera)
                    CameraCaptureScreen(
                        createFile = viewModel::createCaptureFile,
                        onClose = viewModel::closeCamera,
                        onCaptured = viewModel::processCapturedFile,
                        onError = { message ->
                            viewModel.closeCamera()
                            viewModel.showError(message)
                        },
                    )
                }
                state.screen == Screen.Editor && bill != null -> {
                    BackHandler(onBack = viewModel::goHome)
                    BillEditorScreen(
                        bill = bill,
                        selectedPersonKey = state.selectedPersonKey,
                        recentNames = state.recentNames,
                        notice = state.notice,
                        viewModel = viewModel,
                    )
                }
                state.screen == Screen.Summary && bill != null -> {
                    BackHandler(onBack = viewModel::goHome)
                    SummaryScreen(bill = bill, viewModel = viewModel)
                }
                else -> HomeScreen(
                    bills = state.bills,
                    onOpenCamera = viewModel::openCamera,
                    onImport = viewModel::processImport,
                    onManual = viewModel::startManualBill,
                    onOpenBill = viewModel::openBill,
                    onCameraDenied = {
                        viewModel.showError("SplitSnap needs the camera to scan. You can still import a photo or PDF, or type the bill in.")
                    },
                )
            }
        }

        if (state.isProcessing) {
            Dialog(onDismissRequest = {}, properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)) {
                Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp)) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator()
                        Text("Reading the receipt…")
                    }
                }
            }
        }

        state.errorMessage?.let { message ->
            AlertDialog(
                onDismissRequest = viewModel::clearError,
                text = { Text(message) },
                confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } },
            )
        }
    }
}
