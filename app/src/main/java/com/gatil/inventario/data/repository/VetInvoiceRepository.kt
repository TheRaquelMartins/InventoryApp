package com.gatil.inventario.data.repository

import com.gatil.inventario.data.model.VetInvoice
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VetInvoiceRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection("vetInvoices")

    suspend fun getAll(): List<VetInvoice> =
        collection.get().await().toObjects(VetInvoice::class.java)

    suspend fun add(invoice: VetInvoice): Result<Unit> = try {
        collection.add(invoice).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun update(invoice: VetInvoice): Result<Unit> = try {
        require(invoice.id.isNotEmpty()) { "Fatura sem id" }
        collection.document(invoice.id).set(invoice).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun delete(invoiceId: String): Result<Unit> = try {
        collection.document(invoiceId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
