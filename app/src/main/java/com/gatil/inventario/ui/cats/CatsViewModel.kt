package com.gatil.inventario.ui.cats

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.AdoptionStatus
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.repository.CatRepository
import kotlinx.coroutines.launch

class CatsViewModel(
    private val repository: CatRepository = CatRepository()
) : ViewModel() {

    var cats by mutableStateOf<List<Cat>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var statusFilter by mutableStateOf<AdoptionStatus?>(null) // null = todos
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val filteredCats: List<Cat>
        get() {
            var result = cats
            statusFilter?.let { status -> result = result.filter { it.adoptionStatus == status } }
            if (searchQuery.isNotBlank()) {
                result = result.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.litter.contains(searchQuery, ignoreCase = true) ||
                        it.captureLocation.contains(searchQuery, ignoreCase = true)
                }
            }
            return result.sortedByDescending { it.entryDate }
        }

    init {
        load()
    }

    fun onSearchChange(value: String) { searchQuery = value }
    fun onStatusFilterChange(value: AdoptionStatus?) { statusFilter = value }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                cats = repository.getAll()
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar gatos: ${e.message}"
            }
            isLoading = false
        }
    }

    fun deleteCat(cat: Cat) {
        viewModelScope.launch {
            repository.delete(cat.id)
            load()
        }
    }
}
