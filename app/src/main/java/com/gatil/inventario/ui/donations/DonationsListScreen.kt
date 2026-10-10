package com.gatil.inventario.ui.donations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatil.inventario.data.model.DonationItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationsListScreen(
    isAdmin: Boolean,
    onBack: () -> Unit,
    onScanRequest: () -> Unit,
    scannedCode: String?,
    onScanConsumed: () -> Unit,
    viewModel: DonationsViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<DonationItem?>(null) }

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
                title = { Text("Doações") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                    label = { Text("Nome, categoria ou código") },
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
                        DonationItemRow(
                            item = item,
                            isAdmin = isAdmin,
                            onClick = { editingItem = item },
                            onDelete = { viewModel.deleteItem(item.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        DonationDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                viewModel.addItem(item) { showAddDialog = false }
            }
        )
    }

    editingItem?.let { item ->
        DonationDialog(
            initial = item,
            onDismiss = { editingItem = null },
            onSave = { updated ->
                viewModel.updateItem(updated) { editingItem = null }
            }
        )
    }
}

@Composable
private fun DonationItemRow(item: DonationItem, isAdmin: Boolean, onClick: () -> Unit, onDelete: () -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = if (item.isLowStock) {
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        } else {
            CardDefaults.elevatedCardColors()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
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
                    "${item.category} · ${item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.isLowStock) {
                    Text(
                        "Stock baixo — alerta definido para ≤ ${item.lowStockThreshold} ${item.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (item.donor.isNotBlank()) {
                    Text(
                        "Doador: ${item.donor}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                if (item.barcode.isNotBlank()) {
                    Text(
                        "Código: ${item.barcode}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            if (isAdmin) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                }
            }
        }
    }
}

/**
 * Formulário único para criar OU editar um item de doação.
 * Quando `initial` não é nulo, os campos vêm pré-preenchidos e o id/entryDate
 * originais são preservados ao guardar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonationDialog(
    initial: DonationItem?,
    onDismiss: () -> Unit,
    onSave: (DonationItem) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var quantity by remember { mutableStateOf(initial?.quantity?.toString() ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "unidade") }
    var donor by remember { mutableStateOf(initial?.donor ?: "") }
    var barcode by remember { mutableStateOf(initial?.barcode ?: "") }
    var lowStockThreshold by remember {
        mutableStateOf(initial?.lowStockThreshold?.takeIf { it > 0 }?.toString() ?: "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Novo item de doação" else "Editar item de doação") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Categoria") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = quantity, onValueChange = { quantity = it.filter(Char::isDigit) }, label = { Text("Quantidade") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unidade") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = donor, onValueChange = { donor = it }, label = { Text("Doador") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = barcode,
                    onValueChange = { barcode = it },
                    label = { Text("Código de barras/QR (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lowStockThreshold,
                    onValueChange = { lowStockThreshold = it.filter(Char::isDigit) },
                    label = { Text("Alertar quando a quantidade for ≤ (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        (initial ?: DonationItem()).copy(
                            name = name,
                            category = category,
                            quantity = quantity.toIntOrNull() ?: 0,
                            unit = unit,
                            donor = donor,
                            barcode = barcode,
                            lowStockThreshold = lowStockThreshold.toIntOrNull() ?: 0
                        )
                    )
                },
                enabled = name.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
