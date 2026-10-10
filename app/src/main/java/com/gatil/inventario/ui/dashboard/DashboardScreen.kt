package com.gatil.inventario.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

private data class DashboardCard(
    val title: String,
    val icon: ImageVector,
    val lowStockCount: Int,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    isAdmin: Boolean,
    onNavigateDonations: () -> Unit,
    onNavigateCED: () -> Unit,
    onNavigatePartners: () -> Unit,
    onNavigateCats: () -> Unit,
    onNavigateVetAppointments: () -> Unit,
    onNavigateVetInvoices: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = viewModel()
) {
    val cards = listOf(
        DashboardCard("Doações", Icons.Default.Inventory2, viewModel.donationLowStockCount, onNavigateDonations),
        DashboardCard("Material CED", Icons.Default.Build, viewModel.cedLowStockCount, onNavigateCED),
        DashboardCard("Parceiros", Icons.Default.Handshake, 0, onNavigatePartners),
        DashboardCard("Gatos para Adoção", Icons.Default.Pets, 0, onNavigateCats),
        DashboardCard("Consultas Veterinário", Icons.Default.MedicalServices, viewModel.vetAppointmentsSoonCount, onNavigateVetAppointments),
        DashboardCard("Faturas Veterinário", Icons.Default.Receipt, 0, onNavigateVetInvoices)
    )
    val totalLowStock = viewModel.donationLowStockCount + viewModel.cedLowStockCount

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Inventário")
                        Text(
                            if (isAdmin) "Sessão de administrador" else "Sessão de voluntário",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Sair")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (totalLowStock > 0) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 0.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (totalLowStock == 1) "1 item em stock baixo" else "$totalLowStock itens em stock baixo",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(cards) { card ->
                    Box {
                        ElevatedCard(
                            onClick = card.onClick,
                            modifier = Modifier.fillMaxWidth().height(120.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(16.dp),
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(card.icon, contentDescription = null)
                                Spacer(Modifier.height(8.dp))
                                Text(card.title, style = MaterialTheme.typography.titleMedium)
                            }
                        }
                        if (card.lowStockCount > 0) {
                            Badge(
                                containerColor = MaterialTheme.colorScheme.error,
                                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)
                            ) {
                                Text("${card.lowStockCount}")
                            }
                        }
                    }
                }
            }
        }
    }
}
