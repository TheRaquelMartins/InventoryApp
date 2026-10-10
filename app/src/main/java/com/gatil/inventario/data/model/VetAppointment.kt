package com.gatil.inventario.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp

/**
 * Consulta (ou outro evento) de veterinário marcada para um gato do gatil.
 * O nome do gato é guardado em duplicado (tal como acontece com outros
 * registos relacionados na app) para facilitar a listagem sem precisar de
 * ir buscar o documento do gato a cada vez.
 */
data class VetAppointment(
    @DocumentId
    val id: String = "",
    val catId: String = "",
    val catName: String = "",
    val date: Timestamp = Timestamp.now(),
    val vetName: String = "",
    val reason: String = "",          // ex: "Consulta geral", "Vacina", "Esterilização"
    val location: String = "",        // clínica/local
    val notes: String = "",

    // Alerta: quantos dias antes da consulta deve ser emitida a notificação.
    val reminderDaysBefore: Int = 1,
    // Marcado a true depois de a notificação de alerta já ter sido enviada,
    // para não repetir o alerta em cada verificação periódica.
    val reminderSent: Boolean = false
) {
    @get:Exclude
    val isPast: Boolean
        get() = date.toDate().before(java.util.Date())
}
