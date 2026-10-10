package com.gatil.inventario.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Conversão simples entre texto "dd/MM/yyyy" e Timestamp do Firestore,
 * usada nos campos de data opcionais (nascimento, desparasitação, esterilização).
 */
object DateFieldUtil {
    private fun formatter() = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT")).apply {
        isLenient = false
    }

    fun format(timestamp: Timestamp?): String =
        timestamp?.let { formatter().format(it.toDate()) } ?: ""

    /** Devolve null se o texto estiver vazio ou não for uma data válida. */
    fun parse(text: String): Timestamp? {
        if (text.isBlank()) return null
        return try {
            Timestamp(formatter().parse(text) ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    fun isValidOrEmpty(text: String): Boolean = text.isBlank() || parse(text) != null
}
