package com.gatil.inventario.data.repository

import com.gatil.inventario.data.model.DonationItem
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class DonationRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection("donationItems")

    suspend fun getAll(): List<DonationItem> =
        collection.get().await().toObjects(DonationItem::class.java)

    suspend fun add(item: DonationItem): Result<Unit> = try {
        collection.add(item).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun update(item: DonationItem): Result<Unit> = try {
        require(item.id.isNotEmpty()) { "Item sem id" }
        collection.document(item.id).set(item).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun delete(itemId: String): Result<Unit> = try {
        collection.document(itemId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
