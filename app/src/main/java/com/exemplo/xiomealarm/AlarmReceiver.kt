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

        // 1. INICIA O SERVIÇO EM FOREGROUND
        val serviceIntent = Intent(context, AlarmService::class.java).apply {
            putExtra(AlarmService.EXTRA_INTERVAL_MS, intervalMs)
            putExtra(AlarmService.EXTRA_VOLUME_ML, volumeMl)
        }
        ContextCompat.startForegroundService(context, serviceIntent)

        // 2. ABRE A CONSUMEACTIVITY AUTOMATICAMENTE
        val activityIntent = Intent(context, ConsumeActivity::class.java).apply {
            putExtra(AlarmService.EXTRA_VOLUME_ML, volumeMl)
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        context.startActivity(activityIntent)

        // 3. AGENDA O PRÓXIMO ALARME
        scheduleNext(context, intervalMs, volumeMl)
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

        val pending = PendingIntent.getBroadcast(
            context,
            1, //  o RequestCode 1 para evitar conflito com o cancelamento da mainactivity
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