package com.gatil.inventario.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

/**
 * Fatura/recibo de uma despesa numa clínica veterinária. O nome da clínica é
 * texto livre (não há uma entidade "Clínica" separada) — é a partir deste
 * campo que a lista de faturas é agrupada em separadores.
 */
data class VetInvoice(
    @DocumentId
    val id: String = "",
    val clinicName: String = "",
    val catId: String = "",           // opcional: fatura pode não ser de um gato específico
    val catName: String = "",
    val date: Timestamp = Timestamp.now(),
    val amount: Double = 0.0,
    val description: String = "",     // ex: "Consulta + vacina trivalente"
    val photoBase64: String = "",     // foto/scan da fatura, comprimida e guardada no documento (opcional)
    val notes: String = ""
)
