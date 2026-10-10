package com.gatil.inventario.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gatil.inventario.MainActivity
import com.gatil.inventario.R
import com.gatil.inventario.data.model.VetAppointment
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Cria o canal de notificações de consultas de veterinário e emite os
 * alertas individuais. O canal é criado uma vez (é seguro chamar várias
 * vezes) e usado tanto pelo VetReminderWorker como por qualquer outro sítio
 * que precise de emitir este tipo de alerta no futuro.
 */
object NotificationHelper {
    const val CHANNEL_ID = "vet_appointments"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Consultas de veterinário",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas de consultas de veterinário marcadas para os gatos do gatil"
            }
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Mostra o alerta de uma consulta. Não faz nada se a app não tiver
     * permissão de notificações (Android 13+) — evita rebentar com
     * SecurityException se o utilizador tiver recusado a permissão.
     */
    fun showAppointmentReminder(context: Context, appointment: VetAppointment) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        ensureChannel(context)

        val dateText = SimpleDateFormat("dd/MM/yyyy 'às' HH:mm", Locale("pt", "PT"))
            .format(appointment.date.toDate())

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            appointment.id.hashCode(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "Consulta de veterinário: ${appointment.catName}"
        val bodyParts = mutableListOf(dateText)
        if (appointment.vetName.isNotBlank()) bodyParts.add(appointment.vetName)
        if (appointment.location.isNotBlank()) bodyParts.add(appointment.location)
        val body = bodyParts.joinToString(" · ")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_paw)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                if (appointment.reason.isNotBlank()) "$body\n${appointment.reason}" else body
            ))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(appointment.id.hashCode(), notification)
    }
}
