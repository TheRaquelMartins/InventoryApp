package com.gatil.inventario.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Conversão entre texto "dd/MM/yyyy HH:mm" e Timestamp do Firestore, usada no
 * campo de data/hora da consulta de veterinário (tal como DateFieldUtil, mas
 * com hora incluída, já que uma consulta precisa de hora marcada).
 */
object DateTimeFieldUtil {
    private fun formatter() = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "PT")).apply {
        isLenient = false
    }

    fun format(timestamp: Timestamp?): String =
        timestamp?.let { formatter().format(it.toDate()) } ?: ""

    /** Devolve null se o texto estiver vazio ou não for uma data/hora válida. */
    fun parse(text: String): Timestamp? {
        if (text.isBlank()) return null
        return try {
            Timestamp(formatter().parse(text) ?: return null)
        } catch (e: Exception) {
            null
        }
    }

    fun isValid(text: String): Boolean = parse(text) != null
}
