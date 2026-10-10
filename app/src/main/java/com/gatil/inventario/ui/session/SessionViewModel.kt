package com.gatil.inventario.ui.session

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gatil.inventario.data.model.UserProfile
import com.gatil.inventario.data.model.UserRole
import com.gatil.inventario.data.repository.AuthRepository
import kotlinx.coroutines.launch

/**
 * Guarda o perfil do utilizador autenticado (nome, email, role) durante a sessão,
 * para que os ecrãs possam mostrar/esconder ações consoante seja ADMIN ou VOLUNTARIO.
 *
 * Uma única instância é criada em AppNavGraph e partilhada por todos os ecrãs,
 * para não termos de ir buscar o perfil ao Firestore em cada ecrã.
 */
class SessionViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    var profile by mutableStateOf<UserProfile?>(null)
        private set

    val isAdmin: Boolean
        get() = profile?.role == UserRole.ADMIN

    fun loadCurrentUser() {
        viewModelScope.launch {
            profile = authRepository.getCurrentUserProfile()
        }
    }

    fun clear() {
        profile = null
    }
}
