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
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    private val CHANNEL_ID = "water_alarm_channel"

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private var intervalMs: Long = 3600000L
    private var volumeMl: Int = 200

    override fun onCreate() {
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) return START_NOT_STICKY

        intervalMs = intent.getLongExtra("EXTRA_INTERVAL_MS", 3600000L)
        volumeMl = intent.getIntExtra("EXTRA_VOLUME_ML", 200)

        createNotificationChannel()
        startForeground(1, buildNotification())  // 🟦 inicia com notificação bonita

        startAlarmBehavior() // 🔔📳 vibra ou toca

        return START_STICKY
    }

    // ===============================================================
    //   SOM / VIBRAÇÃO
    // ===============================================================
    private fun startAlarmBehavior() {

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val isSilent = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE

        if (isSilent) {
            // 📳 Vibração contínua
            val pattern = longArrayOf(0, 1200, 800) // vibra forte e com "pausa"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }

        } else {
            // 🔔 Som contínuo
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)

            try {
                ringtone?.play()
            } catch (_: Exception) { }
        }
    }

    // ===============================================================
    //   NOTIFICAÇÃO BONITA
    // ===============================================================
    private fun buildNotification(): Notification {

        val openIntent = Intent(this, ConsumeActivity::class.java).apply {
            putExtra("EXTRA_VOLUME_ML", volumeMl)
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
            .setContentTitle("Hora de beber água 💧")
            .setContentText("Beba $volumeMl ml agora!")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setAutoCancel(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembrete de Água",
            NotificationManager.IMPORTANCE_HIGH
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    // ===============================================================
    //   PARAR SOM / VIBRAÇÃO
    // ===============================================================
    override fun onDestroy() {
        ringtone?.stop()
        vibrator?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
