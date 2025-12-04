package com.exemplo.xiomealarm

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import android.widget.Toast
import android.util.Log


class AlarmReceiver : BroadcastReceiver() {

    private val CHANNEL_ID = "water_alarm_channel"

    override fun onReceive(context: Context, intent: Intent) {

        val intervalMs = intent.getLongExtra("EXTRA_INTERVAL_MS", 3600000L) // padrão 1h
        val volumeMl = intent.getIntExtra("EXTRA_DEFAULT_VOLUME_ML", 200)

        val activityIntent = Intent(context, ConsumeActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra("EXTRA_VOLUME_ML", volumeMl)
        }

        context.startActivity(activityIntent)


        // --- Executa o alerta (som + vibração) ---
        triggerAlert(context)



        // --- Envia notificação ---
        sendNotification(context, volumeMl)

        // --- Reagenda automaticamente o próximo alarme ---
        scheduleNextAlarm(context, intervalMs, volumeMl)


    }

    // =====================================================================
    // ALERTA: SOM + VIBRAÇÃO (FUNCIONA EM SILENCIOSO)
    // =====================================================================
    private fun triggerAlert(context: Context) {

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        // vibração sempre funciona, mesmo no silencioso
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val pattern = longArrayOf(0, 1000, 500, 1000) // vibra 1s, pausa 0.5s, vibra 1s
            vibrator.vibrate(
                VibrationEffect.createWaveform(pattern, -1)
            )
        } else {
            vibrator.vibrate(1000)
        }

        // Agora toca som SOMENTE se aparelho NÃO estiver no silencioso
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL) {
            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = RingtoneManager.getRingtone(context, alarmSound)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            }

            ringtone.play()
        }
    }

    // =====================================================================
    // NOTIFICAÇÃO DO ALARME
    // =====================================================================
    private fun sendNotification(context: Context, volumeMl: Int) {

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Criar canal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembrete de Água",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        // 👉 Intent que abre a ActivityConsume ao clicar
        val intent = Intent(context, ConsumeActivity::class.java).apply {
            putExtra("EXTRA_VOLUME_ML", volumeMl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora de beber água 💧")
            .setContentText("Beba ${volumeMl} ml agora!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // 👉 AQUI ESTÁ O SEGREDO
            .build()

        notificationManager.notify(101, notification)
    }


    // =====================================================================
    // REAGENDAR O PRÓXIMO ALARME
    // =====================================================================
    private fun scheduleNextAlarm(context: Context, intervalMs: Long, volumeMl: Int) {

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val newIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.exemplo.xiomealarm.ALARM_TRIGGER"
            putExtra("EXTRA_INTERVAL_MS", intervalMs)
            putExtra("EXTRA_DEFAULT_VOLUME_ML", volumeMl)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            newIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + intervalMs,
                pendingIntent
            )
        } catch (e: SecurityException) {
            Log.e("AlarmReceiver", "Permissão para alarme exato negada: ${e.message}")
            Toast.makeText(context, "O Android bloqueou o alarme exato.", Toast.LENGTH_LONG).show()
        }

    }
}
