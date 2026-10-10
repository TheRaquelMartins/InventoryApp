package com.gatil.inventario.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude

/**
 * Material de Captura, Esterilização e Devolução (CED) de animais errantes.
 * Ex: armadilhas, transportadoras, mantas.
 *
 * totalQuantity = quantos existem no total
 * availableQuantity = quantos estão disponíveis agora (não emprestados)
 * A diferença corresponde ao que está emprestado — ver LoanRecord.
 */
data class CEDItem(
    @DocumentId
    val id: String = "",
    val name: String = "",           // ex: "Armadilha grande", "Transportadora", "Manta"
    val totalQuantity: Int = 0,
    val availableQuantity: Int = 0,
    val condition: String = "Bom",   // "Bom", "Usado", "Danificado", "Em reparação"
    val location: String = "",       // onde está guardado
    val notes: String = "",
    val barcode: String = "",        // código de barras/QR associado (opcional)
    val lowStockThreshold: Int = 0   // alerta quando availableQuantity <= este valor (0 = sem alerta)
) {
    @get:Exclude
    val isLowStock: Boolean
        get() = lowStockThreshold > 0 && availableQuantity <= lowStockThreshold
}
