package com.exemplo.xiomealarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.*
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "ALARM_CHANNEL"
        const val NOTIFICATION_ID = 999
    }

    private var currentRingtone: Ringtone? = null
    private lateinit var vibrator: Vibrator

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        showFullScreenNotification()
        startAlarmSoundAndVibrate()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmSoundAndVibrate()
    }

    override fun onBind(intent: Intent?) = null

    // -----------------------------
    // NOTIFICAÇÃO COM FULL SCREEN
    // -----------------------------
    private fun showFullScreenNotification() {
        val fullScreenIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("fullscreen", true)
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 0, fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Hora de beber água")
            .setContentText("Hidrate-se!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()

        startForeground(NOTIFICATION_ID, notification)
    }

    // -----------------------------
    // CANAL DE NOTIFICAÇÃO CORRETO
    // -----------------------------
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

            val attrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Alarme de Hidratação",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Canal para alarmes de hidratação"
                setSound(alarmSound, attrs)  // <<< CRÍTICO PARA TOCAR NO SILENCIOSO
                enableVibration(true)
            }

            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // -----------------------------
    // SOM + VIBRAÇÃO FUNCIONANDO
    // -----------------------------
    private fun startAlarmSoundAndVibrate() {
        stopAlarmSoundAndVibrate()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val isSilent = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT

        try {
            if (!isSilent) {
                // Só toca som se NÃO estiver no silencioso
                val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

                val attrs = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                currentRingtone = RingtoneManager.getRingtone(applicationContext, alarmUri)
                currentRingtone?.audioAttributes = attrs
                currentRingtone?.play()
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Erro ao tocar som: ${e.message}")
        }

        // Vibra SEMPRE, independente do modo silencioso
        if (vibrator.hasVibrator()) {
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }

        if (vibrator.hasVibrator()) {
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }


    }


    private fun stopAlarmSoundAndVibrate() {
        try {
            currentRingtone?.stop()
        } catch (_: Exception) {}

        vibrator.cancel()
    }
}
