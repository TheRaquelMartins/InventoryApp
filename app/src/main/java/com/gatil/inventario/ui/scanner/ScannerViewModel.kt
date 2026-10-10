package com.gatil.inventario.ui.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Guarda temporariamente o resultado de uma digitalização de código QR/barras
 * enquanto o utilizador navega do ecrã de câmara de volta ao ecrã que pediu
 * a leitura (Doações ou Material CED).
 */
class ScannerViewModel : ViewModel() {

    companion object {
        const val TARGET_DONATIONS = "donations"
        const val TARGET_CED = "ced"
    }

    var pendingCode: String? by mutableStateOf(null)
        private set
    var target: String? by mutableStateOf(null)
        private set

    fun requestScan(target: String) {
        this.target = target
    }

    fun onScanned(code: String) {
        pendingCode = code
    }

    /** Consome (limpa) o resultado pendente depois de o ecrã de destino o tratar. */
    fun consume() {
        pendingCode = null
        target = null
    }
}
