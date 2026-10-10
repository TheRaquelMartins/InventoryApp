package com.gatil.inventario.ui.donations

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.DonationItem
import com.gatil.inventario.data.repository.DonationRepository
import kotlinx.coroutines.launch

class DonationsViewModel(
    private val repository: DonationRepository = DonationRepository()
) : ViewModel() {

    var items by mutableStateOf<List<DonationItem>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var onlyLowStock by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val filteredItems: List<DonationItem>
        get() {
            var result = items
            if (onlyLowStock) result = result.filter { it.isLowStock }
            if (searchQuery.isNotBlank()) {
                result = result.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.category.contains(searchQuery, ignoreCase = true) ||
                        it.barcode.equals(searchQuery, ignoreCase = true)
                }
            }
            // Itens em stock baixo aparecem primeiro, para chamar a atenção
            return result.sortedByDescending { it.isLowStock }
        }

    val lowStockCount: Int
        get() = items.count { it.isLowStock }

    init {
        load()
    }

    fun onSearchChange(value: String) { searchQuery = value }
    fun onToggleLowStockFilter(value: Boolean) { onlyLowStock = value }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                items = repository.getAll()
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar dados: ${e.message}"
            }
            isLoading = false
        }
    }

    fun addItem(item: DonationItem, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.add(item)
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao adicionar item"
            }
        }
    }

    fun updateItem(item: DonationItem, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.update(item)
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao atualizar item"
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.delete(itemId)
            result.onSuccess {
                load()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao eliminar item"
            }
        }
    }
}
