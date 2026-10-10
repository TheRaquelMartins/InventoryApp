package com.gatil.inventario.ui.ced

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.CEDItem
import com.gatil.inventario.data.model.LoanRecord
import com.gatil.inventario.data.repository.CEDRepository
import kotlinx.coroutines.launch

class CEDViewModel(
    private val repository: CEDRepository = CEDRepository()
) : ViewModel() {

    var items by mutableStateOf<List<CEDItem>>(emptyList())
        private set
    var activeLoans by mutableStateOf<List<LoanRecord>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var onlyLowStock by mutableStateOf(false)
        private set

    val filteredItems: List<CEDItem>
        get() {
            var result = items
            if (onlyLowStock) result = result.filter { it.isLowStock }
            if (searchQuery.isNotBlank()) {
                result = result.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.barcode.equals(searchQuery, ignoreCase = true)
                }
            }
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
                items = repository.getAllItems()
                activeLoans = repository.getActiveLoans()
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar dados: ${e.message}"
            }
            isLoading = false
        }
    }

    fun addItem(item: CEDItem, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.addItem(item)
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao adicionar item"
            }
        }
    }

    /**
     * Atualiza nome/localização/estado/quantidade total de um item CED.
     * Se a quantidade total mudar, a disponível é recalculada preservando
     * o número de unidades atualmente emprestadas (não mexe nos empréstimos ativos).
     */
    fun updateItem(original: CEDItem, edited: CEDItem, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            try {
                val loanedQuantity = original.totalQuantity - original.availableQuantity
                val newAvailable = (edited.totalQuantity - loanedQuantity).coerceAtLeast(0)
                val result = repository.updateItem(edited.copy(availableQuantity = newAvailable))
                result.onSuccess {
                    load()
                    onDone()
                }.onFailure {
                    errorMessage = it.message ?: "Erro ao atualizar item"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Erro ao atualizar item"
            }
        }
    }

    fun deleteItem(itemId: String) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.deleteItem(itemId)
            result.onSuccess {
                load()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao eliminar item"
            }
        }
    }

    fun loansForItem(itemId: String): List<LoanRecord> =
        activeLoans.filter { it.cedItemId == itemId }

    fun registerLoan(
        item: CEDItem,
        quantity: Int,
        borrowerName: String,
        borrowerContact: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.registerLoan(item, quantity, borrowerName, borrowerContact, null)
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao registar empréstimo"
            }
        }
    }

    fun registerReturn(loan: LoanRecord) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.registerReturn(loan)
            result.onSuccess {
                load()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao registar devolução"
            }
        }
    }
}
