package com.gatil.inventario.data.repository

import com.gatil.inventario.data.model.VetAppointment
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class VetAppointmentRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val collection = db.collection("vetAppointments")

    suspend fun getAll(): List<VetAppointment> =
        collection.get().await().toObjects(VetAppointment::class.java)

    suspend fun add(appointment: VetAppointment): Result<Unit> = try {
        collection.add(appointment).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun update(appointment: VetAppointment): Result<Unit> = try {
        require(appointment.id.isNotEmpty()) { "Consulta sem id" }
        collection.document(appointment.id).set(appointment).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun delete(appointmentId: String): Result<Unit> = try {
        collection.document(appointmentId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
