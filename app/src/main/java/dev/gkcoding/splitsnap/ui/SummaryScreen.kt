package dev.gkcoding.splitsnap.ui

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.gkcoding.splitsnap.domain.Bill
import dev.gkcoding.splitsnap.domain.Money
import dev.gkcoding.splitsnap.domain.PersonShare
import dev.gkcoding.splitsnap.domain.SplitCalculator
import dev.gkcoding.splitsnap.domain.SplitResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SummaryScreen(bill: Bill, viewModel: SplitViewModel) {
    val context = LocalContext.current
    val result = remember(bill) { SplitCalculator.split(bill) }
    val symbol = bill.currencySymbol
    // The first person is whoever scanned the bill and paid it.
    val payer = bill.people.firstOrNull()
    val owing = result.shares.drop(1).filter { it.totalCents != 0L }
    val paidCount = owing.count { it.person.paid }
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = viewModel::goHome) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = { Text(bill.displayName) },
                actions = {
                    IconButton(onClick = viewModel::editBill) { Icon(Icons.Outlined.Edit, contentDescription = "Edit split") }
                    IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, contentDescription = "Delete bill") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { shareSplit(context, bill, result) },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Send everyone their share")
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            Money.format(bill.totalCents, symbol),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            listOfNotNull(bill.date.ifBlank { null }, "${bill.people.size} people", "${bill.items.size} items").joinToString(" · "),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (payer != null && owing.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            val owed = owing.filterNot { it.person.paid }.sumOf { it.totalCents }
                            Text(
                                if (owed == 0L) "Everyone has paid ${payer.name} back 🎉"
                                else "${payer.name} is still owed ${Money.format(owed, symbol)}",
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            LinearProgressIndicator(
                                progress = { paidCount.toFloat() / owing.size },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            if (result.unassignedCents != 0L) {
                item {
                    Card(
                        onClick = viewModel::editBill,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "${Money.format(result.unassignedCents, symbol)} isn't assigned to anyone yet. Tap to fix.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                }
            }

            items(result.shares, key = { it.person.key }) { share ->
                ShareCard(
                    share = share,
                    symbol = symbol,
                    isPayer = share.person.key == payer?.key,
                    payerName = payer?.name.orEmpty(),
                    onTogglePaid = { viewModel.togglePaid(share.person.key) },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this bill?") },
            text = { Text("The split and the receipt photo are removed from this phone.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; viewModel.deleteBill() }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ShareCard(
    share: PersonShare,
    symbol: String,
    isPayer: Boolean,
    payerName: String,
    onTogglePaid: () -> Unit,
) {
    var expanded by rememberSaveable(share.person.key) { mutableStateOf(false) }

    Card(
        onClick = { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PersonAvatar(share.person, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(share.person.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        when {
                            isPayer -> "Paid the bill"
                            share.items.isEmpty() -> "Nothing assigned"
                            else -> "${share.items.size} item${if (share.items.size == 1) "" else "s"} · tap for details"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    Money.format(share.totalCents, symbol),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = share.person.color(),
                )
            }

            if (!isPayer && share.totalCents != 0L) {
                FilterChip(
                    selected = share.person.paid,
                    onClick = onTogglePaid,
                    label = { Text(if (share.person.paid) "Paid $payerName back" else "Mark as paid") },
                )
            }

            if (expanded) {
                share.items.forEach { item ->
                    BreakdownLine(
                        label = if (item.sharedWith > 1) "${item.itemName}  ÷${item.sharedWith}" else item.itemName,
                        value = Money.format(item.cents, symbol),
                    )
                }
                if (share.taxCents != 0L) BreakdownLine("Tax", Money.format(share.taxCents, symbol))
                if (share.tipCents != 0L) BreakdownLine("Tip", Money.format(share.tipCents, symbol))
            }
        }
    }
}

@Composable
private fun BreakdownLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun shareSplit(context: Context, bill: Bill, result: SplitResult) {
    val symbol = bill.currencySymbol
    val text = buildString {
        appendLine(listOfNotNull(bill.displayName, bill.date.ifBlank { null }).joinToString(" · "))
        appendLine("Total ${Money.format(bill.totalCents, symbol)}")
        appendLine()
        result.shares.forEach { share ->
            append("${share.person.name}: ${Money.format(share.totalCents, symbol)}")
            if (share.items.isNotEmpty()) {
                append("  (")
                append(share.items.joinToString(", ") { if (it.sharedWith > 1) "${it.itemName} ÷${it.sharedWith}" else it.itemName })
                append(")")
            }
            appendLine()
        }
        bill.people.firstOrNull()?.let { appendLine().appendLine("Please send your share to ${it.name}.") }
        append("Split with SplitSnap")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Share split"))
}
