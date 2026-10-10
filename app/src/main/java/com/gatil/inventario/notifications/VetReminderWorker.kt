package com.gatil.inventario.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.Constraints
import com.gatil.inventario.data.repository.VetAppointmentRepository
import java.util.concurrent.TimeUnit

/**
 * Corre periodicamente em segundo plano e verifica se há consultas de
 * veterinário a aproximar-se da data do alerta escolhido (reminderDaysBefore).
 * Quando encontra uma, emite a notificação e marca a consulta como já
 * alertada, para não repetir o aviso na próxima verificação.
 *
 * Corre com uma periodicidade de algumas horas (ver [schedule]) em vez de um
 * alarme exato à hora precisa: isto evita ter de pedir a permissão de
 * "alarmes exatos" ao utilizador e é suficiente para um lembrete de
 * consulta, onde uma margem de algumas horas não é problema.
 */
class VetReminderWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val repository = VetAppointmentRepository()
            val now = System.currentTimeMillis()
            val appointments = repository.getAll()

            for (appointment in appointments) {
                if (appointment.reminderSent) continue

                val appointmentMillis = appointment.date.toDate().time
                if (appointmentMillis <= now) continue // já passou, não alertar

                val millisUntil = appointmentMillis - now
                val reminderWindowMillis =
                    appointment.reminderDaysBefore.coerceAtLeast(0) * 24L * 60L * 60L * 1000L

                if (millisUntil <= reminderWindowMillis) {
                    NotificationHelper.showAppointmentReminder(applicationContext, appointment)
                    repository.update(appointment.copy(reminderSent = true))
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "vet_appointment_reminder_check"

        /**
         * Agenda (ou reaproveita, se já agendado) a verificação periódica.
         * Seguro chamar em todos os arranques da app — o WorkManager não
         * duplica o trabalho graças à política KEEP.
         */
        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED) // precisa de rede para ler o Firestore
                .build()

            val request = PeriodicWorkRequestBuilder<VetReminderWorker>(6, TimeUnit.HOURS)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
