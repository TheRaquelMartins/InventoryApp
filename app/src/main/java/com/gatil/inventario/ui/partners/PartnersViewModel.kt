package com.gatil.inventario.ui.partners

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.Advantage
import com.gatil.inventario.data.model.Partner
import com.gatil.inventario.data.repository.PartnerRepository
import kotlinx.coroutines.launch

class PartnersViewModel(
    private val repository: PartnerRepository = PartnerRepository()
) : ViewModel() {

    var partners by mutableStateOf<List<Partner>>(emptyList())
        private set
    var advantagesByPartner by mutableStateOf<Map<String, List<Advantage>>>(emptyMap())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    val filteredPartners: List<Partner>
        get() = if (searchQuery.isBlank()) partners
        else partners.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                it.type.contains(searchQuery, ignoreCase = true)
        }

    init {
        load()
    }

    fun onSearchChange(value: String) { searchQuery = value }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                partners = repository.getAll()
                val map = mutableMapOf<String, List<Advantage>>()
                partners.forEach { map[it.id] = repository.getAdvantages(it.id) }
                advantagesByPartner = map
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar dados: ${e.message}"
            }
            isLoading = false
        }
    }

    fun addPartner(partner: Partner, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.add(partner)
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao adicionar parceiro"
            }
        }
    }

    fun updatePartner(partner: Partner, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.update(partner)
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao atualizar parceiro"
            }
        }
    }

    fun deletePartner(partnerId: String) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.delete(partnerId)
            result.onSuccess {
                load()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao eliminar parceiro"
            }
        }
    }

    fun addAdvantage(partnerId: String, advantage: Advantage, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.addAdvantage(partnerId, advantage)
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao adicionar vantagem"
            }
        }
    }

    fun deleteAdvantage(partnerId: String, advantageId: String) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.deleteAdvantage(partnerId, advantageId)
            result.onSuccess {
                load()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao eliminar vantagem"
            }
        }
    }
}
