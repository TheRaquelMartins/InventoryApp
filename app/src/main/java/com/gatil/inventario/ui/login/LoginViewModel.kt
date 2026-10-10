package com.gatil.inventario.ui.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    var email by mutableStateOf("")
        private set
    var password by mutableStateOf("")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun onEmailChange(value: String) { email = value }
    fun onPasswordChange(value: String) { password = value }

    fun login(onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank()) {
            errorMessage = "Preenche o email e a palavra-passe"
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = authRepository.login(email.trim(), password)
            isLoading = false
            result.onSuccess { onSuccess() }
                .onFailure { errorMessage = it.message ?: "Erro ao iniciar sessão" }
        }
    }

    fun register(name: String, onSuccess: () -> Unit) {
        if (email.isBlank() || password.isBlank() || name.isBlank()) {
            errorMessage = "Preenche todos os campos"
            return
        }
        viewModelScope.launch {
            isLoading = true
            errorMessage = null
            val result = authRepository.register(name, email.trim(), password)
            isLoading = false
            result.onSuccess { onSuccess() }
                .onFailure { errorMessage = it.message ?: "Erro ao criar conta" }
        }
    }
}
