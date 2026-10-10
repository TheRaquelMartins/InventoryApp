package com.gatil.inventario.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.Timestamp

/**
 * Item recebido como doação (ração, medicamentos, material diverso, etc.)
 */
data class DonationItem(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val category: String = "",       // ex: "Ração", "Medicamentos", "Material"
    val quantity: Int = 0,
    val unit: String = "unidade",    // ex: "kg", "unidade", "pacote"
    val donor: String = "",
    val entryDate: Timestamp = Timestamp.now(),
    val condition: String = "Bom",   // "Bom", "Usado", "Danificado"
    val notes: String = "",
    val barcode: String = "",       // código de barras/QR associado (opcional)
    val lowStockThreshold: Int = 0  // alerta quando quantity <= este valor (0 = sem alerta)
) {
    @get:Exclude
    val isLowStock: Boolean
        get() = lowStockThreshold > 0 && quantity <= lowStockThreshold
}
