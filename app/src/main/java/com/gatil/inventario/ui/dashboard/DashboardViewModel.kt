package com.gatil.inventario.ui.dashboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.repository.CEDRepository
import com.gatil.inventario.data.repository.DonationRepository
import com.gatil.inventario.data.repository.VetAppointmentRepository
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val donationRepository: DonationRepository = DonationRepository(),
    private val cedRepository: CEDRepository = CEDRepository(),
    private val vetAppointmentRepository: VetAppointmentRepository = VetAppointmentRepository()
) : ViewModel() {

    var donationLowStockCount by mutableStateOf(0)
        private set
    var cedLowStockCount by mutableStateOf(0)
        private set
    // Consultas de veterinário nas próximas 48h, para destacar no dashboard.
    var vetAppointmentsSoonCount by mutableStateOf(0)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            errorMessage = null
            try {
                donationLowStockCount = donationRepository.getAll().count { it.isLowStock }
                cedLowStockCount = cedRepository.getAllItems().count { it.isLowStock }

                val now = System.currentTimeMillis()
                val in48h = now + 48L * 60L * 60L * 1000L
                vetAppointmentsSoonCount = vetAppointmentRepository.getAll().count {
                    val millis = it.date.toDate().time
                    millis in now..in48h
                }
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar dados: ${e.message}"
            }
        }
    }
}
