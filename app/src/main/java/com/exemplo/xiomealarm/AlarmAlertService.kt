package com.exemplo.xiomealarm

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.provider.Settings
import androidx.core.app.NotificationCompat

class AlarmService : Service() {

    companion object {
        const val CHANNEL_ID = "water_alarm_channel"
        const val NOTIFICATION_ID = 1
        const val EXTRA_INTERVAL_MS = "EXTRA_INTERVAL_MS"
        const val EXTRA_VOLUME_ML = "EXTRA_VOLUME_ML"

        const val ACTION_STOP = "com.exemplo.xiomealarm.ACTION_STOP"
    }

    private var vibrator: Vibrator? = null

    private var volumeMl: Int = 200

    private var mediaPlayer: MediaPlayer? = null

    private fun openConsumeActivity() {

        val intent = Intent(this, ConsumeActivity::class.java).apply {

            putExtra(EXTRA_VOLUME_ML, volumeMl)

            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        startActivity(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) return START_NOT_STICKY

        volumeMl = intent.getIntExtra(EXTRA_VOLUME_ML, volumeMl)

        if (intent.action == ACTION_STOP) {
            stopAlarmImmediately()
            stopForeground(true)
            stopSelf()
            return START_NOT_STICKY
        }

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        //openConsumeActivity()

        playAlarmSoundIfAllowed()
        startVibrationIfSilent()

        return START_NOT_STICKY
    }

    //     TOCAR SOM FORA DO SILENCIOSO

    private fun playAlarmSoundIfAllowed() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val isSilent =
            audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                    audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE

        if (isSilent) return

        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_ALARM_ALERT_URI)

        mediaPlayer?.apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            isLooping = true
            start()
        }
    }

    //     VIBRAÇÃO APENAS NO SILENCIOSO / VIBRAR

    private fun startVibrationIfSilent() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        val isSilent =
            audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT ||
                    audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE

        if (!isSilent) return

        val pattern = longArrayOf(0, 1000, 500)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createWaveform(pattern, 0)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(pattern, 0)
        }
    }

    //     NOTIFICAÇÃO

    private fun buildNotification(): Notification {

        val intent = Intent(this, ConsumeActivity::class.java).apply {
            putExtra(EXTRA_VOLUME_ML, volumeMl)
            flags =
                Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Hora de beber água")
            .setContentText("Beba $volumeMl ml agora!")
            .setCategory(Notification.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
    }

    //     CANAL DE NOTIFICAÇÃO

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Lembrete de Água",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            /*enableVibration(false)
            vibrationPattern = null*/
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC

            setBypassDnd(true)

            enableLights(true)

            enableVibration(false)
        }

        val manager = getSystemService(NotificationManager::class.java)
        manager?.createNotificationChannel(channel)
    }


    //     LIMPEZA
    private fun stopAlarmImmediately() {
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null

        vibrator?.cancel()
    }


    override fun onBind(intent: Intent?): IBinder? = null
}
