package com.gatil.inventario.ui.ced

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.LoanRecord
import com.gatil.inventario.data.model.LoanStatus
import com.gatil.inventario.data.repository.CEDRepository
import kotlinx.coroutines.launch

enum class LoanHistoryFilter { TODOS, EMPRESTADOS, DEVOLVIDOS }

class LoanHistoryViewModel(
    private val repository: CEDRepository = CEDRepository()
) : ViewModel() {

    var loans by mutableStateOf<List<LoanRecord>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var filter by mutableStateOf(LoanHistoryFilter.TODOS)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val filteredLoans: List<LoanRecord>
        get() = loans
            .filter {
                when (filter) {
                    LoanHistoryFilter.TODOS -> true
                    LoanHistoryFilter.EMPRESTADOS -> it.status == LoanStatus.EMPRESTADO
                    LoanHistoryFilter.DEVOLVIDOS -> it.status == LoanStatus.DEVOLVIDO
                }
            }
            .filter {
                searchQuery.isBlank() ||
                    it.cedItemName.contains(searchQuery, ignoreCase = true) ||
                    it.borrowerName.contains(searchQuery, ignoreCase = true)
            }

    init {
        load()
    }

    fun onSearchChange(value: String) { searchQuery = value }
    fun onFilterChange(value: LoanHistoryFilter) { filter = value }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                loans = repository.getAllLoans()
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar histórico: ${e.message}"
            }
            isLoading = false
        }
    }
}
