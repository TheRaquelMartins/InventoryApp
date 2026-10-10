package com.gatil.inventario

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.gatil.inventario.notifications.NotificationHelper
import com.gatil.inventario.notifications.VetReminderWorker
import com.gatil.inventario.ui.navigation.AppNavGraph
import com.gatil.inventario.ui.theme.InventarioAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cria o canal de notificações e agenda a verificação periódica dos
        // alertas de consultas de veterinário. Seguro chamar em todos os
        // arranques: não duplica trabalho já agendado.
        NotificationHelper.ensureChannel(this)
        VetReminderWorker.schedule(this)

        setContent {
            InventarioAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        ActivityResultContracts.RequestPermission()
                    ) { /* se recusada, os alertas simplesmente não aparecem */ }

                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            val granted = ContextCompat.checkSelfPermission(
                                this@MainActivity,
                                Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                            if (!granted) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    }

                    AppNavGraph()
                }
            }
        }
    }
}
