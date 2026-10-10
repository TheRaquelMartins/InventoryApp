package com.gatil.inventario.util

import java.util.Locale

/**
 * Conversão simples entre texto de um campo de valor monetário e Double,
 * aceitando tanto vírgula como ponto como separador decimal (comum em
 * utilizadores portugueses a escrever num teclado numérico).
 */
object AmountFieldUtil {
    fun parse(text: String): Double? =
        text.trim().replace(",", ".").toDoubleOrNull()

    fun isValid(text: String): Boolean = text.isBlank() || parse(text) != null

    fun format(amount: Double): String = String.format(Locale("pt", "PT"), "%.2f €", amount)
}
