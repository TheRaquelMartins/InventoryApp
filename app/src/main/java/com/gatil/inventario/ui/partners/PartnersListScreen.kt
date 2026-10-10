package com.gatil.inventario.ui.partners

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatil.inventario.data.model.Advantage
import com.gatil.inventario.data.model.Partner

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartnersListScreen(
    isAdmin: Boolean,
    onBack: () -> Unit,
    viewModel: PartnersViewModel = viewModel()
) {
    var showAddPartnerDialog by remember { mutableStateOf(false) }
    var addAdvantageForPartner by remember { mutableStateOf<Partner?>(null) }
    var editingPartner by remember { mutableStateOf<Partner?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Parceiros") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddPartnerDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar parceiro")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Pesquisar por nome ou tipo") },
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.filteredPartners, key = { it.id }) { partner ->
                        PartnerCard(
                            partner = partner,
                            advantages = viewModel.advantagesByPartner[partner.id].orEmpty(),
                            isAdmin = isAdmin,
                            onAddAdvantage = { addAdvantageForPartner = partner },
                            onDeleteAdvantage = { adv -> viewModel.deleteAdvantage(partner.id, adv.id) },
                            onEditPartner = { editingPartner = partner },
                            onDeletePartner = { viewModel.deletePartner(partner.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddPartnerDialog) {
        AddPartnerDialog(
            onDismiss = { showAddPartnerDialog = false },
            onSave = { partner -> viewModel.addPartner(partner) { showAddPartnerDialog = false } }
        )
    }

    addAdvantageForPartner?.let { partner ->
        AddAdvantageDialog(
            partnerName = partner.name,
            onDismiss = { addAdvantageForPartner = null },
            onSave = { advantage ->
                viewModel.addAdvantage(partner.id, advantage) { addAdvantageForPartner = null }
            }
        )
    }

    editingPartner?.let { partner ->
        EditPartnerDialog(
            partner = partner,
            onDismiss = { editingPartner = null },
            onSave = { edited ->
                viewModel.updatePartner(edited) { editingPartner = null }
            }
        )
    }
}

@Composable
private fun PartnerCard(
    partner: Partner,
    advantages: List<Advantage>,
    isAdmin: Boolean,
    onAddAdvantage: () -> Unit,
    onDeleteAdvantage: (Advantage) -> Unit,
    onEditPartner: () -> Unit,
    onDeletePartner: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(partner.name, style = MaterialTheme.typography.titleMedium)
                    Text(partner.type, style = MaterialTheme.typography.bodyMedium)
                    if (partner.contact.isNotBlank()) {
                        Text(partner.contact, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Row {
                    IconButton(onClick = onEditPartner) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar parceiro")
                    }
                    if (isAdmin) {
                        IconButton(onClick = onDeletePartner) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar parceiro")
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { expanded = !expanded }) {
                Text(if (expanded) "Ocultar vantagens (${advantages.size})" else "Ver vantagens (${advantages.size})")
            }

            if (expanded) {
                advantages.forEach { advantage ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(advantage.description, style = MaterialTheme.typography.bodyMedium)
                            if (advantage.conditions.isNotBlank()) {
                                Text(advantage.conditions, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { onDeleteAdvantage(advantage) }, enabled = isAdmin) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar vantagem",
                                tint = if (isAdmin) LocalContentColor.current else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
                TextButton(onClick = onAddAdvantage) { Text("+ Adicionar vantagem") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddPartnerDialog(
    onDismiss: () -> Unit,
    onSave: (Partner) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var contact by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Novo parceiro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Tipo (ex: clínica veterinária)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contacto") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Morada") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(Partner(name = name, type = type, contact = contact, address = address)) },
                enabled = name.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPartnerDialog(
    partner: Partner,
    onDismiss: () -> Unit,
    onSave: (Partner) -> Unit
) {
    var name by remember { mutableStateOf(partner.name) }
    var type by remember { mutableStateOf(partner.type) }
    var contact by remember { mutableStateOf(partner.contact) }
    var address by remember { mutableStateOf(partner.address) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar parceiro") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nome") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Tipo (ex: clínica veterinária)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = contact, onValueChange = { contact = it }, label = { Text("Contacto") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Morada") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(partner.copy(name = name, type = type, contact = contact, address = address)) },
                enabled = name.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAdvantageDialog(
    partnerName: String,
    onDismiss: () -> Unit,
    onSave: (Advantage) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var conditions by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Vantagem — $partnerName") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Descrição (ex: 10% desconto em consultas)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = conditions, onValueChange = { conditions = it }, label = { Text("Condições") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(Advantage(description = description, conditions = conditions)) },
                enabled = description.isNotBlank()
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
