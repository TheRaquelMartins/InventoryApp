package com.gatil.inventario.ui.vet

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.model.VetAppointment
import com.gatil.inventario.data.repository.CatRepository
import com.gatil.inventario.data.repository.VetAppointmentRepository
import kotlinx.coroutines.launch

class VetAppointmentsViewModel(
    private val repository: VetAppointmentRepository = VetAppointmentRepository(),
    private val catRepository: CatRepository = CatRepository()
) : ViewModel() {

    var appointments by mutableStateOf<List<VetAppointment>>(emptyList())
        private set
    var cats by mutableStateOf<List<Cat>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var showPastAppointments by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** Próximas primeiro; passadas ficam ao fundo (só visíveis se pedido). */
    val visibleAppointments: List<VetAppointment>
        get() {
            val list = if (showPastAppointments) appointments else appointments.filterNot { it.isPast }
            return list.sortedBy { it.date }
        }

    val upcomingSoonCount: Int
        get() {
            val now = System.currentTimeMillis()
            val in48h = now + 48L * 60L * 60L * 1000L
            return appointments.count {
                val millis = it.date.toDate().time
                millis in now..in48h
            }
        }

    init {
        load()
    }

    fun onToggleShowPast(value: Boolean) { showPastAppointments = value }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                appointments = repository.getAll()
                cats = catRepository.getAll()
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar dados: ${e.message}"
            }
            isLoading = false
        }
    }

    fun addAppointment(appointment: VetAppointment, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            // Nova consulta -> garante que o alerta ainda não foi enviado.
            val result = repository.add(appointment.copy(reminderSent = false))
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao adicionar consulta"
            }
        }
    }

    fun updateAppointment(appointment: VetAppointment, onDone: () -> Unit) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.update(appointment)
            result.onSuccess {
                load()
                onDone()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao atualizar consulta"
            }
        }
    }

    fun deleteAppointment(appointmentId: String) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.delete(appointmentId)
            result.onSuccess {
                load()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao eliminar consulta"
            }
        }
    }
}
