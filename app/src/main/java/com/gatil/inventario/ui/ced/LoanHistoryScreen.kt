package com.gatil.inventario.ui.ced

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gatil.inventario.data.model.LoanRecord
import com.gatil.inventario.data.model.LoanStatus
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

private val dateFormatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT"))

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanHistoryScreen(
    onBack: () -> Unit,
    viewModel: LoanHistoryViewModel = viewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Histórico de empréstimos") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Pesquisar por material ou pessoa") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                val options = listOf(
                    LoanHistoryFilter.TODOS to "Todos",
                    LoanHistoryFilter.EMPRESTADOS to "Emprestados",
                    LoanHistoryFilter.DEVOLVIDOS to "Devolvidos"
                )
                options.forEachIndexed { index, (filterValue, label) ->
                    SegmentedButton(
                        selected = viewModel.filter == filterValue,
                        onClick = { viewModel.onFilterChange(filterValue) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) {
                        Text(label)
                    }
                }
            }

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.filteredLoans.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sem registos para mostrar", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.filteredLoans, key = { it.id }) { loan ->
                        LoanHistoryRow(loan)
                    }
                }
            }
        }
    }
}

@Composable
private fun LoanHistoryRow(loan: LoanRecord) {
    val isReturned = loan.status == LoanStatus.DEVOLVIDO

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${loan.cedItemName} · qtd ${loan.quantity}",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "Levado por: ${loan.borrowerName}" +
                        if (loan.borrowerContact.isNotBlank()) " (${loan.borrowerContact})" else "",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Empréstimo: ${formatTimestamp(loan.loanDate)}",
                    style = MaterialTheme.typography.bodySmall
                )
                if (isReturned && loan.actualReturnDate != null) {
                    Text(
                        "Devolvido: ${formatTimestamp(loan.actualReturnDate)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Icon(
                    imageVector = if (isReturned) Icons.Default.CheckCircle else Icons.Default.Schedule,
                    contentDescription = null,
                    tint = if (isReturned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
                Text(
                    if (isReturned) "Devolvido" else "Emprestado",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Timestamp): String =
    dateFormatter.format(timestamp.toDate())
