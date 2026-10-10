package com.gatil.inventario.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

data class Partner(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val type: String = "",         // ex: "Clínica veterinária", "Loja de animais", "Empresa"
    val contact: String = "",
    val address: String = "",
    val notes: String = ""
)

/**
 * Vantagem/benefício associado a um parceiro (ex: desconto, serviço gratuito).
 * Guardada como subcoleção de Partner: partners/{partnerId}/advantages/{advantageId}
 */
data class Advantage(
    @DocumentId
    val id: String = "",
    val description: String = "",
    val conditions: String = "",
    val validUntil: Timestamp? = null   // null = sem validade definida
)
