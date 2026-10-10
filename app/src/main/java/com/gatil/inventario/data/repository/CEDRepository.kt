package com.gatil.inventario.data.repository

import com.gatil.inventario.data.model.CEDItem
import com.gatil.inventario.data.model.LoanRecord
import com.gatil.inventario.data.model.LoanStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CEDRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val itemsCollection = db.collection("cedItems")
    private val loansCollection = db.collection("loanRecords")

    suspend fun getAllItems(): List<CEDItem> =
        itemsCollection.get().await().toObjects(CEDItem::class.java)

    suspend fun addItem(item: CEDItem): Result<Unit> = try {
        // ao criar, disponível = total
        val toSave = item.copy(availableQuantity = item.totalQuantity)
        itemsCollection.add(toSave).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateItem(item: CEDItem): Result<Unit> = try {
        require(item.id.isNotEmpty()) { "Item sem id" }
        itemsCollection.document(item.id).set(item).await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteItem(itemId: String): Result<Unit> = try {
        itemsCollection.document(itemId).delete().await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getLoansForItem(cedItemId: String): List<LoanRecord> =
        loansCollection.whereEqualTo("cedItemId", cedItemId).get().await()
            .toObjects(LoanRecord::class.java)

    suspend fun getActiveLoans(): List<LoanRecord> =
        loansCollection.whereEqualTo("status", LoanStatus.EMPRESTADO.name).get().await()
            .toObjects(LoanRecord::class.java)

    /**
     * Histórico completo de empréstimos (emprestados e já devolvidos),
     * ordenado do mais recente para o mais antigo.
     */
    suspend fun getAllLoans(): List<LoanRecord> =
        loansCollection.orderBy("loanDate", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await().toObjects(LoanRecord::class.java)

    /**
     * Regista um empréstimo: cria o LoanRecord e decrementa availableQuantity
     * do CEDItem, tudo numa transação atómica.
     */
    suspend fun registerLoan(
        cedItem: CEDItem,
        quantity: Int,
        borrowerName: String,
        borrowerContact: String,
        expectedReturnDate: Timestamp?
    ): Result<Unit> = try {
        require(quantity in 1..cedItem.availableQuantity) { "Quantidade indisponível" }
        db.runTransaction { transaction ->
            val itemRef = itemsCollection.document(cedItem.id)
            val snapshot = transaction.get(itemRef)
            val currentAvailable = snapshot.getLong("availableQuantity")?.toInt() ?: 0
            if (quantity > currentAvailable) {
                throw IllegalStateException("Quantidade indisponível")
            }
            transaction.update(itemRef, "availableQuantity", currentAvailable - quantity)

            val loanRef = loansCollection.document()
            val loan = LoanRecord(
                id = loanRef.id,
                cedItemId = cedItem.id,
                cedItemName = cedItem.name,
                quantity = quantity,
                borrowerName = borrowerName,
                borrowerContact = borrowerContact,
                loanDate = Timestamp.now(),
                expectedReturnDate = expectedReturnDate,
                status = LoanStatus.EMPRESTADO
            )
            transaction.set(loanRef, loan)
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    /**
     * Regista a devolução: marca o LoanRecord como devolvido e repõe
     * availableQuantity do CEDItem, numa transação atómica.
     */
    suspend fun registerReturn(loan: LoanRecord): Result<Unit> = try {
        require(loan.status == LoanStatus.EMPRESTADO) { "Empréstimo já devolvido" }
        db.runTransaction { transaction ->
            val itemRef = itemsCollection.document(loan.cedItemId)
            val loanRef = loansCollection.document(loan.id)

            val snapshot = transaction.get(itemRef)
            val currentAvailable = snapshot.getLong("availableQuantity")?.toInt() ?: 0
            val totalQuantity = snapshot.getLong("totalQuantity")?.toInt() ?: 0
            val newAvailable = (currentAvailable + loan.quantity).coerceAtMost(totalQuantity)
            transaction.update(itemRef, "availableQuantity", newAvailable)

            transaction.update(
                loanRef,
                mapOf(
                    "status" to LoanStatus.DEVOLVIDO.name,
                    "actualReturnDate" to Timestamp.now()
                )
            )
        }.await()
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }
}
