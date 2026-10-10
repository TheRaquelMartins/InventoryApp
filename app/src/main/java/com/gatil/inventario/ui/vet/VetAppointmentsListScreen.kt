package com.gatil.inventario.ui.vet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.model.VetAppointment
import com.gatil.inventario.util.DateTimeFieldUtil

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VetAppointmentsListScreen(
    isAdmin: Boolean,
    onBack: () -> Unit,
    viewModel: VetAppointmentsViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var editingAppointment by remember { mutableStateOf<VetAppointment?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Consultas de Veterinário") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Marcar consulta")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = viewModel.showPastAppointments,
                    onClick = { viewModel.onToggleShowPast(!viewModel.showPastAppointments) },
                    label = { Text("Mostrar consultas passadas") }
                )
            }

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.visibleAppointments.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.EventBusy,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Sem consultas marcadas",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.visibleAppointments, key = { it.id }) { appointment ->
                        AppointmentRow(
                            appointment = appointment,
                            isAdmin = isAdmin,
                            onClick = { editingAppointment = appointment },
                            onDelete = { viewModel.deleteAppointment(appointment.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        VetAppointmentDialog(
            initial = null,
            cats = viewModel.cats,
            onDismiss = { showAddDialog = false },
            onSave = { appointment ->
                viewModel.addAppointment(appointment) { showAddDialog = false }
            }
        )
    }

    editingAppointment?.let { appointment ->
        VetAppointmentDialog(
            initial = appointment,
            cats = viewModel.cats,
            onDismiss = { editingAppointment = null },
            onSave = { updated ->
                viewModel.updateAppointment(updated) { editingAppointment = null }
            }
        )
    }
}

@Composable
private fun AppointmentRow(
    appointment: VetAppointment,
    isAdmin: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val now = remember { System.currentTimeMillis() }
    val isSoon = !appointment.isPast && (appointment.date.toDate().time - now) <= 48L * 60L * 60L * 1000L

    ElevatedCard(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = when {
            appointment.isPast -> CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
            isSoon -> CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            )
            else -> CardDefaults.elevatedCardColors()
        }
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        appointment.catName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (isSoon) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            Icons.Default.NotificationsActive,
                            contentDescription = "Consulta a aproximar-se",
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Text(
                    DateTimeFieldUtil.format(appointment.date),
                    style = MaterialTheme.typography.bodyMedium
                )
                if (appointment.reason.isNotBlank()) {
                    Text(appointment.reason, style = MaterialTheme.typography.bodySmall)
                }
                if (appointment.vetName.isNotBlank() || appointment.location.isNotBlank()) {
                    Text(
                        listOf(appointment.vetName, appointment.location)
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    "Alerta: ${appointment.reminderDaysBefore} dia(s) antes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
 * Formulário único para marcar OU editar uma consulta.
 * Ao editar, se a data for alterada, o alerta é reposto a "por enviar" para
 * que a notificação seja emitida novamente em função da nova data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VetAppointmentDialog(
    initial: VetAppointment?,
    cats: List<Cat>,
    onDismiss: () -> Unit,
    onSave: (VetAppointment) -> Unit
) {
    var catExpanded by remember { mutableStateOf(false) }
    var selectedCat by remember {
        mutableStateOf(cats.firstOrNull { it.id == initial?.catId })
    }
    var catNameFallback by remember { mutableStateOf(initial?.catName ?: "") }

    var dateText by remember { mutableStateOf(DateTimeFieldUtil.format(initial?.date)) }
    var vetName by remember { mutableStateOf(initial?.vetName ?: "") }
    var reason by remember { mutableStateOf(initial?.reason ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var reminderDaysBefore by remember {
        mutableStateOf((initial?.reminderDaysBefore ?: 1).toString())
    }

    val dateValid = DateTimeFieldUtil.isValid(dateText)
    val catValid = selectedCat != null || catNameFallback.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nova consulta de veterinário" else "Editar consulta") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (cats.isEmpty()) {
                    // Sem gatos registados ainda: permite escrever o nome à mão.
                    OutlinedTextField(
                        value = catNameFallback,
                        onValueChange = { catNameFallback = it },
                        label = { Text("Nome do gato") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    ExposedDropdownMenuBox(
                        expanded = catExpanded,
                        onExpandedChange = { catExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCat?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Gato") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = catExpanded,
                            onDismissRequest = { catExpanded = false }
                        ) {
                            cats.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = {
                                        selectedCat = cat
                                        catExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = dateText,
                    onValueChange = { dateText = it },
                    label = { Text("Data e hora (dd/MM/aaaa HH:mm)") },
                    isError = dateText.isNotBlank() && !dateValid,
                    supportingText = {
                        if (dateText.isNotBlank() && !dateValid) {
                            Text("Data/hora inválida")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo (ex: consulta geral, vacina)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = vetName,
                    onValueChange = { vetName = it },
                    label = { Text("Veterinário/clínica") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Local (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = reminderDaysBefore,
                    onValueChange = { reminderDaysBefore = it.filter(Char::isDigit) },
                    label = { Text("Alertar quantos dias antes") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notas (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val parsedDate = DateTimeFieldUtil.parse(dateText) ?: return@TextButton
                    val dateChanged = initial?.date != parsedDate
                    onSave(
                        (initial ?: VetAppointment()).copy(
                            catId = selectedCat?.id ?: initial?.catId ?: "",
                            catName = selectedCat?.name ?: catNameFallback,
                            date = parsedDate,
                            vetName = vetName,
                            reason = reason,
                            location = location,
                            notes = notes,
                            reminderDaysBefore = reminderDaysBefore.toIntOrNull() ?: 1,
                            reminderSent = if (dateChanged) false else (initial?.reminderSent ?: false)
                        )
                    )
                },
                enabled = dateValid && catValid
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
