package com.gatil.inventario.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.Timestamp

enum class LoanStatus { EMPRESTADO, DEVOLVIDO }

/**
 * Registo de empréstimo de um CEDItem: quem levou, quando, e se já devolveu.
 */
data class LoanRecord(
    @DocumentId
    val id: String = "",
    val cedItemId: String = "",
    val cedItemName: String = "",     // guardado em duplicado para facilitar listagem
    val quantity: Int = 1,
    val borrowerName: String = "",
    val borrowerContact: String = "",
    val loanDate: Timestamp = Timestamp.now(),
    val expectedReturnDate: Timestamp? = null,
    val actualReturnDate: Timestamp? = null,
    val status: LoanStatus = LoanStatus.EMPRESTADO,
    val notes: String = ""
)
