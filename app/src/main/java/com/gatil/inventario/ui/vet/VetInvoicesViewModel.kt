package com.gatil.inventario.ui.vet

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.model.VetInvoice
import com.gatil.inventario.data.repository.CatRepository
import com.gatil.inventario.data.repository.VetInvoiceRepository
import com.gatil.inventario.util.ImageCompressionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VetInvoicesViewModel(
    private val repository: VetInvoiceRepository = VetInvoiceRepository(),
    private val catRepository: CatRepository = CatRepository()
) : ViewModel() {

    var invoices by mutableStateOf<List<VetInvoice>>(emptyList())
        private set
    var cats by mutableStateOf<List<Cat>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** null representa o separador "Todas". */
    var selectedClinic by mutableStateOf<String?>(null)
        private set

    /** Nomes de clínica distintos, ordenados alfabeticamente, um por separador. */
    val clinics: List<String>
        get() = invoices.map { it.clinicName }
            .filter { it.isNotBlank() }
            .distinct()
            .sortedBy { it.lowercase() }

    val visibleInvoices: List<VetInvoice>
        get() {
            val list = if (selectedClinic == null) invoices else invoices.filter { it.clinicName == selectedClinic }
            return list.sortedByDescending { it.date }
        }

    val visibleTotal: Double
        get() = visibleInvoices.sumOf { it.amount }

    init {
        load()
    }

    fun onSelectClinic(clinic: String?) { selectedClinic = clinic }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            try {
                invoices = repository.getAll()
                cats = catRepository.getAll()
                // Se a clínica selecionada deixou de ter faturas, volta a "Todas".
                if (selectedClinic != null && clinics.none { it == selectedClinic }) {
                    selectedClinic = null
                }
            } catch (e: Exception) {
                errorMessage = "Erro ao carregar dados: ${e.message}"
            }
            isLoading = false
        }
    }

    /**
     * Comprime a foto localmente (se houver uma nova) e devolve o texto Base64
     * a guardar, ou null com `errorMessage` preenchido se falhar.
     */
    private suspend fun resolvePhoto(context: Context, currentPhotoBase64: String, newPhotoUri: Uri?): String? {
        if (newPhotoUri == null) return currentPhotoBase64
        val compressed = withContext(Dispatchers.IO) {
            ImageCompressionUtil.compressToBase64(context, newPhotoUri)
        }
        if (compressed == null) {
            errorMessage = "Não foi possível processar a foto escolhida"
        }
        return compressed
    }

    fun addInvoice(context: Context, invoice: VetInvoice, photoUri: Uri?, onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            errorMessage = null

            val photoBase64 = resolvePhoto(context, invoice.photoBase64, photoUri)
            if (photoUri != null && photoBase64 == null) {
                isSaving = false
                return@launch
            }

            val result = repository.add(invoice.copy(photoBase64 = photoBase64 ?: ""))
            isSaving = false
            result.onSuccess {
                load()
                onDone()
            }.onFailure { errorMessage = it.message ?: "Erro ao guardar" }
        }
    }

    fun updateInvoice(context: Context, invoice: VetInvoice, photoUri: Uri?, onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            errorMessage = null

            val photoBase64 = resolvePhoto(context, invoice.photoBase64, photoUri)
            if (photoUri != null && photoBase64 == null) {
                isSaving = false
                return@launch
            }

            val result = repository.update(invoice.copy(photoBase64 = photoBase64 ?: ""))
            isSaving = false
            result.onSuccess {
                load()
                onDone()
            }.onFailure { errorMessage = it.message ?: "Erro ao guardar" }
        }
    }

    fun deleteInvoice(invoice: VetInvoice) {
        viewModelScope.launch {
            errorMessage = null
            val result = repository.delete(invoice.id)
            result.onSuccess {
                load()
            }.onFailure {
                errorMessage = it.message ?: "Erro ao eliminar fatura"
            }
        }
    }
}
