package com.exemplo.xiomealarm

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import android.app.AlarmManager
import android.app.PendingIntent
import android.util.Log

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmReceiver", "Alarme recebido. Iniciando Service.")

        val intervalMs = intent.getLongExtra(AlarmService.EXTRA_INTERVAL_MS, 3600000L)
        val volumeMl = intent.getIntExtra(AlarmService.EXTRA_VOLUME_ML, 200)

        // 1. INICIA O SERVIÇO EM FOREGROUND (para notificar e vibrar)
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmService.EXTRA_INTERVAL_MS, intervalMs)
            putExtra(AlarmService.EXTRA_VOLUME_ML, volumeMl)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        //  AGENDA O PRÓXIMO ALARME IMEDIATAMENTE.
        // Se esta chamada não for feita, o alarme não dispara novamente.
        scheduleNext(context, intervalMs, volumeMl)
        Log.d("AlarmReceiver", "Próximo alarme agendado para $intervalMs ms.")
    }

    @SuppressLint("ScheduleExactAlarm")
    private fun scheduleNext(context: Context, intervalMs: Long, volumeMl: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            // Mantenha a ação para fins de logging/rastreamento
            action = "com.exemplo.xiomealarm.ALARM_TRIGGER"
            putExtra(AlarmService.EXTRA_INTERVAL_MS, intervalMs)
            putExtra(AlarmService.EXTRA_VOLUME_ML, volumeMl)
        }

        // Use o mesmo RequestCode (0) que você usou em MainActivity, mas é importante
        // garantir que este PendingIntent seja único. Se o MainActivity usar o código 0,
        // ele pode estar sobrescrevendo, mas para o agendamento periódico, usaremos 1.
        val pending = PendingIntent.getBroadcast(
            context,
            1, //  o RequestCode '1' para evitar conflito com o cancelamento da MainActivity
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMs,
            pending
        )
    }

}