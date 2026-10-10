package com.gatil.inventario.ui.cats

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.Cat
import com.gatil.inventario.data.repository.CatRepository
import com.gatil.inventario.util.ImageCompressionUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CatFormViewModel(
    private val repository: CatRepository = CatRepository()
) : ViewModel() {

    var isSaving by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /**
     * Guarda o gato. Se `newPhotoUri` não for nulo, comprime a foto localmente
     * e guarda-a como texto Base64 dentro do próprio documento (sem upload
     * a nenhum serviço externo).
     */
    fun save(context: Context, cat: Cat, newPhotoUri: Uri?, onDone: () -> Unit) {
        viewModelScope.launch {
            isSaving = true
            errorMessage = null

            var photoBase64 = cat.photoBase64
            if (newPhotoUri != null) {
                val compressed = withContext(Dispatchers.IO) {
                    ImageCompressionUtil.compressToBase64(context, newPhotoUri)
                }
                if (compressed == null) {
                    errorMessage = "Não foi possível processar a foto escolhida"
                    isSaving = false
                    return@launch
                }
                photoBase64 = compressed
            }

            val toSave = cat.copy(photoBase64 = photoBase64)
            val result = if (cat.id.isBlank()) repository.add(toSave) else repository.update(toSave)
            isSaving = false
            result.onSuccess { onDone() }
                .onFailure { errorMessage = it.message ?: "Erro ao guardar" }
        }
    }
}
