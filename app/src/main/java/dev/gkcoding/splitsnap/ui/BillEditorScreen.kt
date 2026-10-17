package dev.gkcoding.splitsnap.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.gkcoding.splitsnap.domain.Bill
import dev.gkcoding.splitsnap.domain.BillItem
import dev.gkcoding.splitsnap.domain.Money
import dev.gkcoding.splitsnap.domain.Person
import kotlin.math.abs
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillEditorScreen(
    bill: Bill,
    selectedPersonKey: Long?,
    recentNames: List<String>,
    notice: String?,
    viewModel: SplitViewModel,
) {
    var editingItem by remember { mutableStateOf<BillItem?>(null) }
    var editingPerson by remember { mutableStateOf<Person?>(null) }
    var addingPerson by remember { mutableStateOf(false) }
    var editingDetails by remember { mutableStateOf(false) }
    val selectedPerson = bill.people.firstOrNull { it.key == selectedPersonKey }
    val symbol = bill.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = viewModel::goHome) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Column {
                        Text(bill.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (bill.date.isNotBlank()) {
                            Text(bill.date, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { editingDetails = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit bill details")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = viewModel::showSummary,
                    enabled = bill.items.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(16.dp)
                        .height(52.dp),
                ) {
                    Text(
                        if (bill.unassignedItems.isEmpty()) "See the split · ${Money.format(bill.totalCents, symbol)}"
                        else "See the split · ${bill.unassignedItems.size} unassigned",
                    )
                }
            }
        },
    ) { padding ->
        LazyColumn(
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                bottom = padding.calculateBottomPadding() + 16.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            notice?.let { message ->
                item {
                    NoticeCard(message, onDismiss = viewModel::dismissNotice)
                }
            }

            item {
                SectionTitle("Who's splitting?")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(bill.people, key = { it.key }) { person ->
                        PersonChip(
                            person = person,
                            selected = person.key == selectedPersonKey,
                            onClick = { viewModel.selectPerson(person.key) },
                            onLongClick = { editingPerson = person },
                        )
                    }
                    item {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(56.dp)) {
                            IconButton(
                                onClick = { addingPerson = true },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .padding(0.dp),
                            ) {
                                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                                    Icon(Icons.Outlined.PersonAdd, contentDescription = "Add person", modifier = Modifier.padding(12.dp))
                                }
                            }
                            Text("Add", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Text(
                    text = when {
                        selectedPerson != null -> "Now tap everything ${selectedPerson.name} had. Tap again to undo."
                        bill.people.size < 2 -> "Add the people at the table, then pick someone and tap their items."
                        else -> "Pick a person, then tap their items. Shared dishes can have several people."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selectedPerson != null) selectedPerson.color() else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (selectedPerson != null) FontWeight.SemiBold else FontWeight.Normal,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Items", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    if (bill.unassignedItems.isNotEmpty() && bill.people.isNotEmpty()) {
                        TextButton(onClick = viewModel::splitRestEvenly) { Text("Share the rest with everyone") }
                    }
                }
            }

            items(bill.items, key = { it.key }) { item ->
                ItemRow(
                    item = item,
                    people = bill.people,
                    symbol = symbol,
                    selectedPerson = selectedPerson,
                    onClick = { if (selectedPerson != null) viewModel.onItemTapped(item.key) else editingItem = item },
                    onEdit = { editingItem = item },
                )
            }

            item {
                OutlinedButton(
                    onClick = { editingItem = BillItem(key = -1, name = "", priceCents = 0) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add item")
                }
            }

            item {
                TotalsCard(bill = bill, onTaxChange = viewModel::setTax, onTipChange = viewModel::setTip)
            }
        }
    }

    editingItem?.let { item ->
        ItemDialog(
            item = item,
            people = bill.people,
            symbol = symbol,
            onSave = { viewModel.saveItem(it); editingItem = null },
            onDelete = { viewModel.deleteItem(item.key); editingItem = null },
            onDismiss = { editingItem = null },
        )
    }

    if (addingPerson) {
        PersonNameDialog(
            title = "Add person",
            initial = "",
            suggestions = recentNames.filter { name -> bill.people.none { it.name.equals(name, ignoreCase = true) } },
            onConfirm = { viewModel.addPerson(it); addingPerson = false },
            onDismiss = { addingPerson = false },
        )
    }

    editingPerson?.let { person ->
        PersonNameDialog(
            title = "Edit ${person.name}",
            initial = person.name,
            suggestions = emptyList(),
            onConfirm = { viewModel.renamePerson(person.key, it); editingPerson = null },
            onDismiss = { editingPerson = null },
            onRemove = { viewModel.removePerson(person.key); editingPerson = null },
        )
    }

    if (editingDetails) {
        DetailsDialog(
            bill = bill,
            onConfirm = { merchant, date, currency ->
                viewModel.updateDetails(merchant, date, currency)
                editingDetails = false
            },
            onDismiss = { editingDetails = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
    )
}

@Composable
private fun NoticeCard(message: String, onDismiss: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null)
            Spacer(Modifier.width(10.dp))
            Text(message, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onDismiss) { Text("OK") }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PersonChip(person: Person, selected: Boolean, onClick: () -> Unit, onLongClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .widthIn(min = 56.dp, max = 72.dp)
            .clip(RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick, onLongClickLabel = "Edit person"),
    ) {
        PersonAvatar(person = person, size = 48.dp, selected = selected)
        Text(
            person.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ItemRow(
    item: BillItem,
    people: List<Person>,
    symbol: String,
    selectedPerson: Person?,
    onClick: () -> Unit,
    onEdit: () -> Unit,
) {
    val assignees = people.filter { it.key in item.assignees }
    val highlighted = selectedPerson != null && selectedPerson.key in item.assignees
    val container = when {
        highlighted -> selectedPerson!!.color().copy(alpha = 0.16f)
        else -> MaterialTheme.colorScheme.surfaceContainerLow
    }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = container),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(item.name.ifBlank { "Item" }, style = MaterialTheme.typography.bodyLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (assignees.isEmpty()) {
                    Text("Unassigned", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarStack(assignees, size = 20.dp, max = 6)
                        if (assignees.size > 1) {
                            Text(
                                "÷${assignees.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
            Text(Money.format(item.priceCents, symbol), fontWeight = FontWeight.SemiBold)
            IconButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = "Edit ${item.name}", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TotalsCard(bill: Bill, onTaxChange: (Long) -> Unit, onTipChange: (Long) -> Unit) {
    val symbol = bill.currencySymbol
    val tipOptions = listOf(0, 10, 15, 20)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth().padding(16.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            TotalLine("Subtotal", Money.format(bill.subtotalCents, symbol))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MoneyField(bill.taxCents, onTaxChange, "Tax (if added)", symbol, Modifier.weight(1f))
                MoneyField(bill.tipCents, onTipChange, "Tip", symbol, Modifier.weight(1f))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                tipOptions.forEach { percent ->
                    val cents = (bill.subtotalCents * percent / 100.0).roundToLong()
                    FilterChip(
                        selected = bill.tipCents == cents && (percent == 0 || cents != 0L),
                        onClick = { onTipChange(cents) },
                        label = { Text(if (percent == 0) "No tip" else "$percent%") },
                    )
                }
            }
            HorizontalDivider()
            TotalLine("Total", Money.format(bill.totalCents, symbol), bold = true)

            bill.receiptTotalCents?.let { receiptTotal ->
                val difference = bill.totalCents - receiptTotal
                val matches = abs(difference) <= 1
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (matches) Icons.Outlined.CheckCircle else Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = if (matches) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (matches) "Matches the receipt total"
                        else "Receipt says ${Money.format(receiptTotal, symbol)}, ${Money.format(abs(difference), symbol)} ${if (difference > 0) "over" else "short"}. Check items, tax, and tip.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun TotalLine(label: String, value: String, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold)
    }
}

@Composable
private fun ItemDialog(
    item: BillItem,
    people: List<Person>,
    symbol: String,
    onSave: (BillItem) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val isNew = item.key < 0
    var name by remember { mutableStateOf(item.name) }
    var priceText by remember { mutableStateOf(if (isNew) "" else Money.toInput(item.priceCents)) }
    var assignees by remember { mutableStateOf(item.assignees) }
    val price = Money.parseCents(priceText)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isNew) "Add item" else "Edit item") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("Price") },
                    prefix = { Text(symbol) },
                    isError = priceText.isNotBlank() && price == null,
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (people.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Who had it?", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        TextButton(onClick = {
                            assignees = if (assignees.size == people.size) emptySet() else people.map { it.key }.toSet()
                        }) { Text(if (assignees.size == people.size) "Nobody" else "Everyone") }
                    }
                    people.forEach { person ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    assignees = if (person.key in assignees) assignees - person.key else assignees + person.key
                                },
                        ) {
                            Checkbox(checked = person.key in assignees, onCheckedChange = null, modifier = Modifier.padding(8.dp))
                            PersonAvatar(person, size = 24.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(person.name)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(item.copy(name = name.trim(), priceCents = price ?: 0, assignees = assignees)) },
                enabled = name.isNotBlank() && price != null,
            ) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!isNew) TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PersonNameDialog(
    title: String,
    initial: String,
    suggestions: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    onRemove: (() -> Unit)? = null,
) {
    var name by remember { mutableStateOf(initial) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (suggestions.isNotEmpty()) {
                    Text("Recent", style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        suggestions.forEach { suggestion ->
                            AssistChip(onClick = { onConfirm(suggestion) }, label = { Text(suggestion) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(if (onRemove == null) "Add" else "Save") }
        },
        dismissButton = {
            Row {
                if (onRemove != null) TextButton(onClick = onRemove) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}

@Composable
private fun DetailsDialog(bill: Bill, onConfirm: (String, String, String) -> Unit, onDismiss: () -> Unit) {
    var merchant by remember { mutableStateOf(bill.merchant) }
    var date by remember { mutableStateOf(bill.date) }
    var currency by remember { mutableStateOf(bill.currencySymbol) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bill details") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(merchant, { merchant = it }, label = { Text("Place") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(date, { date = it }, label = { Text("Date") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    currency,
                    { currency = it.take(3) },
                    label = { Text("Currency symbol") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(merchant, date, currency) }) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
