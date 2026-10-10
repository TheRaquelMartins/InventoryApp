package com.gatil.inventario.ui.cats

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.gatil.inventario.data.model.AdoptionStatus
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.model.CatSex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatsListScreen(
    isAdmin: Boolean,
    onBack: () -> Unit,
    viewModel: CatsViewModel = viewModel()
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedCat by remember { mutableStateOf<Cat?>(null) }
    var editingCat by remember { mutableStateOf<Cat?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gatos para Adoção") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar gato")
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = viewModel::onSearchChange,
                label = { Text("Nome, ninhada ou local de captura") },
                modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 8.dp)
            )

            val statusOptions = listOf<AdoptionStatus?>(null) + AdoptionStatus.entries
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statusOptions.forEach { status ->
                    FilterChip(
                        selected = viewModel.statusFilter == status,
                        onClick = { viewModel.onStatusFilterChange(status) },
                        label = { Text(statusLabel(status)) }
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            if (viewModel.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (viewModel.filteredCats.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Sem gatos para mostrar", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(viewModel.filteredCats, key = { it.id }) { cat ->
                        CatCard(cat = cat, onClick = { selectedCat = cat })
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        CatFormDialog(
            initial = null,
            onDismiss = { showAddDialog = false },
            onSaved = { showAddDialog = false; viewModel.load() }
        )
    }

    editingCat?.let { cat ->
        CatFormDialog(
            initial = cat,
            onDismiss = { editingCat = null },
            onSaved = { editingCat = null; selectedCat = null; viewModel.load() }
        )
    }

    selectedCat?.let { cat ->
        CatDetailDialog(
            cat = cat,
            isAdmin = isAdmin,
            onDismiss = { selectedCat = null },
            onEdit = { editingCat = cat },
            onDelete = {
                viewModel.deleteCat(cat)
                selectedCat = null
            }
        )
    }
}

private fun statusLabel(status: AdoptionStatus?): String = when (status) {
    null -> "Todos"
    AdoptionStatus.DISPONIVEL -> "Disponível"
    AdoptionStatus.RESERVADO -> "Reservado"
    AdoptionStatus.ADOTADO -> "Adotado"
}

@Composable
private fun CatCard(cat: Cat, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                CatPhoto(
                    photoBase64 = cat.photoBase64,
                    contentDescription = cat.name,
                    modifier = Modifier.fillMaxSize()
                )
                AssistChip(
                    onClick = {},
                    label = { Text(statusLabel(cat.adoptionStatus), style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = when (cat.sex) {
                        CatSex.MACHO -> Icons.Default.Male
                        CatSex.FEMEA -> Icons.Default.Female
                        CatSex.DESCONHECIDO -> Icons.Default.QuestionMark
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    cat.name.ifBlank { "(sem nome)" },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1
                )
            }
        }
    }
}
