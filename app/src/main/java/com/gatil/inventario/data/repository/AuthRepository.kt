package com.gatil.inventario.data.repository

import com.gatil.inventario.data.model.UserProfile
import com.gatil.inventario.data.model.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    val currentUserId: String? get() = auth.currentUser?.uid

    suspend fun login(email: String, password: String): Result<Unit> = try {
        auth.signInWithEmailAndPassword(email, password).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun register(name: String, email: String, password: String): Result<Unit> = try {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val uid = result.user?.uid ?: throw IllegalStateException("Falha ao criar utilizador")
        val profile = UserProfile(uid = uid, name = name, email = email, role = UserRole.VOLUNTARIO)
        db.collection("users").document(uid).set(profile).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getCurrentUserProfile(): UserProfile? {
        val uid = currentUserId ?: return null
        return try {
            db.collection("users").document(uid).get().await()
                .toObject(UserProfile::class.java)
        } catch (e: Exception) {
            null
        }
    }

    fun logout() = auth.signOut()
}
