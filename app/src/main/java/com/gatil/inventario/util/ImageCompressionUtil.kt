package com.gatil.inventario.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * Comprime e reduz fotos para as guardar diretamente como texto (Base64)
 * dentro do próprio documento do Firestore — evita precisar do Firebase
 * Storage (que agora exige plano pago) para um caso de uso tão simples.
 * Usado tanto pelas fotos dos gatos como pelas fotos/scans das faturas
 * de veterinário.
 *
 * Um documento do Firestore tem um limite de 1MB, por isso reduzimos a
 * imagem para no máximo ~640px no lado maior e comprimimos como JPEG,
 * baixando a qualidade automaticamente até ficar bem abaixo desse limite
 * (deixando margem para os restantes campos de texto do documento).
 */
object ImageCompressionUtil {
    private const val MAX_DIMENSION = 640
    private const val MAX_BYTES = 500_000 // ~500KB de imagem -> ~670KB em Base64, com boa margem no documento

    /** Devolve a foto comprimida como texto Base64, ou null se não for possível processar. */
    fun compressToBase64(context: Context, uri: Uri): String? {
        val bitmap = decodeSampledBitmap(context, uri) ?: return null
        var quality = 80
        var bytes: ByteArray
        do {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            bytes = out.toByteArray()
            quality -= 15
        } while (bytes.size > MAX_BYTES && quality > 20)
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Descodifica o texto Base64 guardado de volta para uma Bitmap, para mostrar no ecrã. */
    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOptions)

        var sampleSize = 1
        while (boundsOptions.outWidth / sampleSize > MAX_DIMENSION * 2 ||
            boundsOptions.outHeight / sampleSize > MAX_DIMENSION * 2
        ) {
            sampleSize *= 2
        }

        val decodedBitmap = BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sampleSize }
        ) ?: return null

        val ratio = minOf(
            MAX_DIMENSION.toFloat() / decodedBitmap.width,
            MAX_DIMENSION.toFloat() / decodedBitmap.height,
            1f
        )
        return if (ratio < 1f) {
            Bitmap.createScaledBitmap(
                decodedBitmap,
                (decodedBitmap.width * ratio).toInt(),
                (decodedBitmap.height * ratio).toInt(),
                true
            )
        } else {
            decodedBitmap
        }
    }
}
