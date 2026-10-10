package com.gatil.inventario.data.repository

import com.gatil.inventario.data.model.Cat
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CatRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection("cats")

    suspend fun getAll(): List<Cat> =
        collection.get().await().toObjects(Cat::class.java)

    suspend fun add(cat: Cat): Result<Unit> = try {
        collection.add(cat).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun update(cat: Cat): Result<Unit> = try {
        require(cat.id.isNotEmpty()) { "Gato sem id" }
        collection.document(cat.id).set(cat).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun delete(catId: String): Result<Unit> = try {
        collection.document(catId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
