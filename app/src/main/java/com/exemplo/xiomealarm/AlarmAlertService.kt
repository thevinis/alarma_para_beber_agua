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
import android.util.Log
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "water_alarm_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_INTERVAL_MS = "EXTRA_INTERVAL_MS"
        const val EXTRA_VOLUME_ML = "EXTRA_VOLUME_ML"

        private const val HANDLER_STOP_DELAY_MS = 40000L
    }

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())

    private var intervalMs: Long = 3600000L
    private var volumeMl: Int = 200

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent == null) return START_NOT_STICKY

        intervalMs = intent.getLongExtra(EXTRA_INTERVAL_MS, 3600000L)
        volumeMl = intent.getIntExtra(EXTRA_VOLUME_ML, 200)

        createNotificationChannel()

        val nm = getSystemService(NotificationManager::class.java)
        Log.i("AlarmService", "notificationsEnabled=${nm?.areNotificationsEnabled()}")
        val ch = nm?.getNotificationChannel(CHANNEL_ID)
        Log.i("AlarmService", "channel=${ch?.id} importance=${ch?.importance} name=${ch?.name}")


        startForeground(NOTIFICATION_ID, buildNotification())

        startAlarmBehavior()

        handler.postDelayed({
            stopAlarmBehavior()
            stopSelf()
        }, HANDLER_STOP_DELAY_MS)

        return START_NOT_STICKY
    }

    // -------------------------------------------------------------
    //     DETECÇÃO DE MIUI (Xiaomi / Redmi / Poco)
    // -------------------------------------------------------------
    private fun isXiaomi(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val brand = Build.BRAND.lowercase()
        val model = Build.MODEL.lowercase()

        return manufacturer.contains("xiaomi") ||
                brand.contains("xiaomi") ||
                brand.contains("redmi") ||
                brand.contains("poco") ||
                model.contains("miui")
    }


    // -------------------------------------------------------------
    //     ALARME (Ringtone + volume reduzido em Xiaomi)
    // -------------------------------------------------------------
    private fun startAlarmBehavior() {

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val isSilent = audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE

        if (isSilent) {
            // Vibração imediata para modo silencioso
            val pattern = longArrayOf(0L, 1200L, 800L)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }

        } else {
            // Som de alarme
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ringtone = RingtoneManager.getRingtone(this, uri)

            try { ringtone?.isLooping = true } catch (_: Throwable) {}

            ringtone?.play()
        }
    }


    // -------------------------------------------------------------
    //     NOTIFICAÇÃO (PRIORIDADE + FULLSCREEN INTENT)
    // -------------------------------------------------------------
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
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setVibrate(longArrayOf(0))
            .setSound(null)
            .setFullScreenIntent(pendingIntent, true)   // 👈 HEADS-UP INSTANTÂNEO
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembrete de Água",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.setSound(null, null)
        channel.enableVibration(true)

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }

    private fun stopAlarmBehavior() {
        try { ringtone?.stop() } catch (_: Exception) {}
        try { vibrator?.cancel() } catch (_: Exception) {}
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        stopAlarmBehavior()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
