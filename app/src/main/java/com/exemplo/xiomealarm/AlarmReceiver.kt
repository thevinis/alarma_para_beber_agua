package com.exemplo.xiomealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        // Pega os extras enviados pelo scheduler
        val intervalMs = intent.getLongExtra("EXTRA_INTERVAL_MS", 3600000L)
        val volumeMl = intent.getIntExtra("EXTRA_VOLUME_ML", 200)

        // Inicia o serviço responsável por:
        // - Som
        // - Vibração
        // - Notificação
        // - Abrir a ConsumeActivity ao clicar
        // - Manter o alarme ativo corretamente
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra("EXTRA_INTERVAL_MS", intervalMs)
            putExtra("EXTRA_VOLUME_ML", volumeMl)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(serviceIntent)
        } else {
            context.startService(serviceIntent)
        }
    }
}
