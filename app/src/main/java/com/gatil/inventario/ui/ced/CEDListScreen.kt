package com.gatil.inventario.ui.ced

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatil.inventario.data.model.CEDItem
import com.gatil.inventario.data.model.LoanRecord
import com.gatil.inventario.util.QrCodeGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CEDListScreen(
    isAdmin: Boolean,
    onBack: () -> Unit,
    onNavigateHistory: () -> Unit,
    onScanRequest: () -> Unit,
    scannedCode: String?,
    onScanConsumed: () -> Unit,
    viewModel: CEDViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var loanDialogItem by remember { mutableStateOf<CEDItem?>(null) }
    var editingItem by remember { mutableStateOf<CEDItem?>(null) }
    var labelItem by remember { mutableStateOf<CEDItem?>(null) }

    // Quando voltamos do ecrã de câmara com um código lido, usa-o para pesquisar
    // imediatamente na lista (forma rápida de encontrar um item já registado).
    LaunchedEffect(scannedCode) {
        if (scannedCode != null) {
            viewModel.onSearchChange(scannedCode)
            onScanConsumed()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Material CED") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateHistory) {
                        Icon(Icons.Default.History, contentDescription = "Histórico de empréstimos")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.searchQuery,
                    onValueChange = viewModel::onSearchChange,
                    label = { Text("Nome ou código") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onScanRequest) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = "Ler código de barras/QR")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = viewModel.onlyLowStock,
                    onClick = { viewModel.onToggleLowStockFilter(!viewModel.onlyLowStock) },
                    label = { Text("Só stock baixo (${viewModel.lowStockCount})") },
                    leadingIcon = if (viewModel.lowStockCount > 0) {
                        { Icon(Icons.Default.WarningAmber, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null
                )
            }
            Spacer(Modifier.height(4.dp))

            viewModel.errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.filteredItems, key = { it.id }) { item ->
                        CEDItemCard(
                            item = item,
                            activeLoans = viewModel.loansForItem(item.id),
                            isAdmin = isAdmin,
                            onLoan = { loanDialogItem = item },
                            onReturn = { loan -> viewModel.registerReturn(loan) },
                            onEdit = { editingItem = item },
                            onShowLabel = { labelItem = item },
                            onDelete = { viewModel.deleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddCEDItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { item -> viewModel.addItem(item) { showAddDialog = false } }
        )
    }

    loanDialogItem?.let { item ->
        LoanDialog(
            item = item,
            onDismiss = { loanDialogItem = null },
            onConfirm = { qty, name, contact ->
                viewModel.registerLoan(item, qty, name, contact) { loanDialogItem = null }
            }
        )
    }

    editingItem?.let { item ->
        EditCEDItemDialog(
            item = item,
            onDismiss = { editingItem = null },
            onSave = { edited ->
                viewModel.updateItem(item, edited) { editingItem = null }
            }
        )
    }

    labelItem?.let { item ->
        QrLabelDialog(item = item, onDismiss = { labelItem = null })
    }
}

@Composable
private fun CEDItemCard(
    item: CEDItem,
    activeLoans: List<LoanRecord>,
    isAdmin: Boolean,
    onLoan: () -> Unit,
    onReturn: (LoanRecord) -> Unit,
    onEdit: () -> Unit,
    onShowLabel: () -> Unit,
    onDelete: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = if (item.isLowStock) {
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.elevatedCardColors()
        }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium)
                        if (item.isLowStock) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = "Stock baixo",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Text(
                        "Disponível: ${item.availableQuantity} / ${item.totalQuantity} · ${item.condition}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (item.isLowStock) {
                        Text(
                            "Stock baixo — alerta definido para ≤ ${item.lowStockThreshold} disponível(is)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (item.location.isNotBlank()) {
                        Text("Local: ${item.location}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (item.barcode.isNotBlank()) {
                        Text("Código: ${item.barcode}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row {
                    IconButton(onClick = onShowLabel) {
                        Icon(Icons.Default.QrCode2, contentDescription = "Ver etiqueta QR")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                    if (isAdmin) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Button(
                onClick = onLoan,
                enabled = item.availableQuantity > 0,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Registar empréstimo")
            }

            if (activeLoans.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Emprestado atualmente:", style = MaterialTheme.typography.labelLarge)
                activeLoans.forEach { loan ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${loan.borrowerName} — qtd ${loan.quantity}", style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { onReturn(loan) }) { Text("Devolver") }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddCEDItemDialog(
    onDismiss: () -> Unit,
    onSave: (CEDItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var totalQuantity by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var condition by remember { mutableStateOf("Bom") }
    var barcode by remember { mutableStateOf("") }
    var lowStockThreshold by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo material CED") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome (ex: Armadilha grande)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = totalQuantity, onValueChange = { totalQuantity = it.filter(Char::isDigit) }, label = { Text("Quantidade total") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Localização") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = condition, onValueChange = { condition = it }, label = { Text("Estado") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Código de barras/QR (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it.filter(Char::isDigit) },
                    label = { Text("Alertar quando disponível for ≤ (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "Dica: podes deixar em branco e gerar uma etiqueta QR própria depois de guardar (ícone de QR no cartão do item).",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = totalQuantity.toIntOrNull() ?: 0
                    onSave(
                        CEDItem(
                            name = name,
                            totalQuantity = qty,
                            availableQuantity = qty,
                            condition = condition,
                            location = location,
                            barcode = barcode,
                            lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 0
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditCEDItemDialog(
    item: CEDItem,
    onDismiss: () -> Unit,
    onSave: (CEDItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var totalQuantity by remember { mutableStateOf(item.totalQuantity.toString()) }
    var location by remember { mutableStateOf(item.location) }
    var condition by remember { mutableStateOf(item.condition) }
    var barcode by remember { mutableStateOf(item.barcode) }
    var lowStockThreshold by remember {
        mutableStateOf(item.lowStockThreshold.takeIf { it > 0 }?.toString() ?: "")
    }
    val loanedQuantity = item.totalQuantity - item.availableQuantity

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar material CED") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = totalQuantity, onValueChange = { totalQuantity = it.filter(Char::isDigit) }, label = { Text("Quantidade total") }, modifier = Modifier.fillMaxWidth())
                if (loanedQuantity > 0) {
                    Text(
                        "$loanedQuantity atualmente emprestado(s) — a quantidade disponível será ajustada automaticamente.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                OutlinedTextField(value = location, onValueChange = { location = it }, label = { Text("Localização") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = condition, onValueChange = { condition = it }, label = { Text("Estado") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Código de barras/QR (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it.filter(Char::isDigit) },
                    label = { Text("Alertar quando disponível for ≤ (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val newTotal = totalQuantity.toIntOrNull() ?: item.totalQuantity
                    onSave(
                        item.copy(
                            name = name,
                            totalQuantity = newTotal,
                            location = location,
                            condition = condition,
                            barcode = barcode,
                            lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 0
                        )
                    )
                },
                enabled = name.isNotBlank() && (totalQuantity.toIntOrNull() ?: 0) >= loanedQuantity
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDialog(
    item: CEDItem,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int, borrowerName: String, borrowerContact: String) -> Unit
) {
    var quantity by remember { mutableStateOf("1") }
    var borrowerName by remember { mutableStateOf("") }
    var borrowerContact by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Empréstimo — ${item.name}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Disponível: ${item.availableQuantity}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter(Char::isDigit) }, label = { Text("Quantidade") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = borrowerName, onValueChange = { borrowerName = it }, label = { Text("Nome de quem leva") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = borrowerContact, onValueChange = { borrowerContact = it }, label = { Text("Contacto") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 0
                    if (qty in 1..item.availableQuantity && borrowerName.isNotBlank()) {
                        onConfirm(qty, borrowerName, borrowerContact)
                    }
                },
                enabled = borrowerName.isNotBlank() && (quantity.toIntOrNull() ?: 0) in 1..item.availableQuantity
            ) { Text("Confirmar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

/**
 * Mostra um código QR gerado a partir do código do item (barcode, se definido,
 * senão o id do documento). Pode ser fotografado/impresso e colado no material
 * físico para depois ser lido rapidamente com o botão de scan da lista.
 */
@Composable
private fun QrLabelDialog(item: CEDItem, onDismiss: () -> Unit) {
    val content = item.barcode.ifBlank { item.id }
    val bitmap = remember(content) { QrCodeGenerator.generate(content) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Etiqueta QR — ${item.name}") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Código QR de ${item.name}",
                    modifier = Modifier.size(220.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(content, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tira um print screen desta imagem para imprimir e colar no material.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fechar") }
        }
    )
}
