package com.gatil.inventario.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

enum class CatSex { MACHO, FEMEA, DESCONHECIDO }

enum class AdoptionStatus { DISPONIVEL, RESERVADO, ADOTADO }

/**
 * Gato disponível para adoção (ou já adotado/reservado, para manter histórico).
 * Muitos campos são opcionais porque, sendo animais capturados na rua,
 * nem sempre há informação completa (ex: data de nascimento exata).
 */
data class Cat(
    @DocumentId
    val id: String = "",
    val name: String = "",
    val sex: CatSex = CatSex.DESCONHECIDO,
    val photoBase64: String = "",   // foto comprimida embutida no documento (sem Firebase Storage)

    // Idade / origem
    val birthDate: Timestamp? = null,        // preencher se a data exata for conhecida
    val estimatedAgeNotes: String = "",       // ex: "aprox. 3 meses", quando não há data exata
    val litter: String = "",                  // ninhada (para agrupar irmãos)
    val captureLocation: String = "",

    // Aparência
    val color: String = "",                   // cor/pelagem

    // Saúde
    val sterilized: Boolean = false,
    val sterilizedDate: Timestamp? = null,
    val externalDewormingDate: Timestamp? = null,
    val internalDewormingDate: Timestamp? = null,
    val vaccinated: Boolean = false,
    val vaccinationNotes: String = "",         // ex: "Trivalente 12/03/2026, reforço marcado"
    val fivFelvTested: Boolean = false,
    val fivFelvResult: String = "",            // ex: "Negativo", "Positivo FIV"
    val microchipNumber: String = "",
    val healthNotes: String = "",

    // Comportamento / adoção
    val temperament: String = "",              // ex: "Sociável, brincalhão, bom com outros gatos"
    val adoptionStatus: AdoptionStatus = AdoptionStatus.DISPONIVEL,
    val entryDate: Timestamp = Timestamp.now(),
    val notes: String = ""
)
