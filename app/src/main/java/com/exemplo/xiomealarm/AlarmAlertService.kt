package com.exemplo.xiomealarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "water_alarm_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_INTERVAL_MS = "EXTRA_INTERVAL_MS"
        const val EXTRA_VOLUME_ML = "EXTRA_VOLUME_ML"

        // Tempo limite de execução do alarme (10 segundos)
        private const val HANDLER_STOP_DELAY_MS = 20000L
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private var intervalMs: Long = 3600000L
    private var volumeMl: Int = 200

    override fun onCreate() {
        super.onCreate()
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) return START_NOT_STICKY

        intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 3600000L)
        volumeMl = intent.getIntExtra(EXTRA_VOLUME_ML, 200)

        createNotificationChannel()

        // 1. Inicia o serviço em primeiro plano com a notificação
        startForeground(NOTIFICATION_ID, buildNotification())

        // 2. Inicia o alarme IMEDIATAMENTE (crucial para Xiaomi/MIUI).
        startAlarmBehavior()

        // 3. Agenda a interrupção do serviço após 10 segundos
        handler.postDelayed({
            stopAlarmBehavior()
            stopSelf()
        }, 10000L)

        return START_NOT_STICKY
    }

    // ===============================================================
    //   SOM / VIBRAÇÃO (Prioridade máxima com TYPE_ALARM)
    // ===============================================================
    private fun startAlarmBehavior() {

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val isSilent = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE

        if (isSilent) {
            // Vibração manual e imediata (máxima prioridade tátil)
            val pattern = longArrayOf(0L, 1200L, 800L)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }

        }
        else {
            // Toca som de ALARME (máxima prioridade de áudio)
            try {
                val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ringtone = RingtoneManager.getRingtone(this, uri)
                try {
                    ringtone?.isLooping = true
                } catch (_: Throwable) {}
                ringtone?.play()
            } catch (_: Exception) {
                // fallback
                try {
                    val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ringtone = RingtoneManager.getRingtone(this, uri)
                    ringtone?.play()
                } catch (_: Exception) {}
            }
        }
    }


    private fun buildNotification(): Notification {

        val openIntent = Intent(this, ConsumeActivity::class.java).apply {
            putExtra(EXTRA_VOLUME_ML, volumeMl)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            200,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora de beber água")
            .setContentText("Beba $volumeMl ml agora!")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX) // 🚨 Prioridade de visualização máxima (Heads-up)
            .setCategory(Notification.CATEGORY_ALARM) // 🚨 CATEGORIZAÇÃO: Trata a notificação como ALARME
            .setOngoing(true)
            .setVibrate(longArrayOf(0L))
            .setSound(null)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembrete de Água",
            NotificationManager.IMPORTANCE_HIGH // 🚨 Nível de importância máximo para o canal
        )
        channel.setSound(null, null)
        channel.enableVibration(true)
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    // ===============================================================
    //   PARAR SOM / VIBRAÇÃO
    // ===============================================================
    private fun stopAlarmBehavior() {
        try {
            ringtone?.stop()
        } catch (_: Exception) {}
        try {
            vibrator?.cancel()
        } catch (_: Exception) {}
    }


    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopAlarmBehavior()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}