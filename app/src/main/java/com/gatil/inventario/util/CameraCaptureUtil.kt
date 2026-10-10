package com.gatil.inventario.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

/**
 * Cria um ficheiro temporário (em cache) e devolve um content:// Uri através
 * do FileProvider, para usar como destino da app de câmara do sistema
 * (ActivityResultContracts.TakePicture).
 */
object CameraCaptureUtil {
    fun createTempImageUri(context: Context): Uri {
        val dir = File(context.cacheDir, "cat_photos").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    }
}
