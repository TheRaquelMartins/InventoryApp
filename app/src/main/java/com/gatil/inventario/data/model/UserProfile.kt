package com.gatil.inventario.data.model

import com.google.firebase.firestore.DocumentId

enum class UserRole { ADMIN, VOLUNTARIO }

data class UserProfile(
    @DocumentId
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val role: UserRole = UserRole.VOLUNTARIO
)
