package com.exemplo.xiomealarm

import android.app.Notification // NOVO!
import android.app.NotificationChannel // NOVO!
import android.app.NotificationManager // NOVO!
import android.app.PendingIntent // NOVO!
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat // NOVO!

class AlarmService : Service() {

    // Constantes para o Foreground Service
    private val NOTIFICATION_CHANNEL_ID = "alarm_service_channel"
    private val NOTIFICATION_ID = 101

    // VARIÁVEIS DO SERVICE
    private var currentRingtone: Ringtone? = null
    private var currentVibrator: Vibrator? = null

    // Função que cria o canal de notificação (obrigatório a partir do Android 8.0)
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Alarme em Execução",
                NotificationManager.IMPORTANCE_HIGH // Garante que a notificação seja visível
            ).apply {
                description = "Notificação de serviço para garantir que o alarme toque."
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    // Função que constrói a Notificação
    private fun buildNotification(): Notification {
        // Intent para retornar à MainActivity ao clicar na notificação
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Alarme Horário Ativo")
            .setContentText("Toque para desativar o alarme.")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Use um ícone adequado
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true) // Remove a notificação quando clicada
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i("XiomeAlarm", "AlarmService Iniciado.")

        // 1. CHAMA O FOREGROUND SERVICE (GARANTE QUE O SERVIÇO VIVA)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        // 2. Toca o alarme e inicia a vibração
        startAlarmLogic(applicationContext)

        // 3. Inicia a tela de parada (Activity)
        startAlarmStopActivity()

        return START_STICKY
    }

    private fun startAlarmLogic(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // Garante que qualquer alarme anterior seja parado
        stopAlarmLogic()

        // --- LÓGICA DE SOM ---
        if (audioManager.ringerMode != AudioManager.RINGER_MODE_SILENT) {
            try {
                val alarmUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

                val ringtone = RingtoneManager.getRingtone(context, alarmUri)
                ringtone.play()
                currentRingtone = ringtone
            } catch (e: Exception) {
                Log.e("XiomeAlarm", "Erro ao tocar som no Service: ${e.message}")
            }
        }

        // --- LÓGICA DE VIBRAÇÃO (Silencioso ou Vibrar) ---
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE || audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT) {

            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            try {
                val pattern = longArrayOf(0, 700, 500, 700)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(pattern, 0)
                }

                currentVibrator = vibrator
            } catch (e: Exception) {
                Log.e("XiomeAlarm", "Erro ao vibrar no Service: ${e.message}")
            }
        }
    }

    private fun startAlarmStopActivity() {
        val stopIntent = Intent(applicationContext, AlarmStopActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(stopIntent)
    }

    // Parar o som/vibração e o serviço
    private fun stopAlarmLogic() {
        currentRingtone?.stop()
        currentRingtone = null
        currentVibrator?.cancel()
        currentVibrator = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAlarmLogic()
        Log.i("XiomeAlarm", "AlarmService Parado e recursos liberados.")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        fun stopService(context: Context) {
            val stopIntent = Intent(context, AlarmService::class.java)
            context.stopService(stopIntent)
        }
    }
}