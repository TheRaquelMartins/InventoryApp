package com.gatil.inventario.data.repository

import com.gatil.inventario.data.model.Advantage
import com.gatil.inventario.data.model.Partner
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class PartnerRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection("partners")

    suspend fun getAll(): List<Partner> =
        collection.get().await().toObjects(Partner::class.java)

    suspend fun add(partner: Partner): Result<Unit> = try {
        collection.add(partner).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun update(partner: Partner): Result<Unit> = try {
        require(partner.id.isNotEmpty()) { "Parceiro sem id" }
        collection.document(partner.id).set(partner).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun delete(partnerId: String): Result<Unit> = try {
        collection.document(partnerId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getAdvantages(partnerId: String): List<Advantage> =
        collection.document(partnerId).collection("advantages")
            .get().await().toObjects(Advantage::class.java)

    suspend fun addAdvantage(partnerId: String, advantage: Advantage): Result<Unit> = try {
        collection.document(partnerId).collection("advantages").add(advantage).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateAdvantage(partnerId: String, advantage: Advantage): Result<Unit> = try {
        require(advantage.id.isNotEmpty()) { "Vantagem sem id" }
        collection.document(partnerId).collection("advantages")
            .document(advantage.id).set(advantage).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteAdvantage(partnerId: String, advantageId: String): Result<Unit> = try {
        collection.document(partnerId).collection("advantages")
            .document(advantageId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
