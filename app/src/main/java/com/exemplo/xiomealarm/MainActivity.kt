package com.exemplo.xiomealarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// Esta classe gerencia a interface do usuário e agenda/cancela o alarme.

class MainActivity : AppCompatActivity() {

    // Constante para identificar o alarme
    private val ALARM_REQUEST_CODE = 100

    // DEFINA AQUI SEU INTERVALO (em milissegundos)
    // 2 * 60 * 1000L = 2 minutos
    // 60 * 60 * 1000L = 1 hora (para uso normal)
    private val ALARM_INTERVAL_MS = 60 * 60 * 1000L // Intervalo de 2 minutos

    private lateinit var alarmManager: AlarmManager
    private lateinit var pendingIntent: PendingIntent
    private lateinit var alarmToggle: Switch
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Define o layout usando o arquivo activity_main.xml
        setContentView(R.layout.activity_main)

        // Inicializa as views
        alarmToggle = findViewById(R.id.alarmToggle)
        statusText = findViewById(R.id.statusText)

        // 1. Inicializa o AlarmManager
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 2. Cria o PendingIntent para o AlarmReceiver
        val intent = Intent(this, AlarmReceiver::class.java)
        // FLAG_IMMUTABLE é necessário para Android 12+
        pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. Verifica o status inicial (se o alarme já está agendado)
        // FLAG_NO_CREATE retorna null se o PendingIntent não existir
        val isAlarmSet = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) != null

        alarmToggle.isChecked = isAlarmSet
        updateStatusText(isAlarmSet)

        // 4. Listener para o botão de ativação/desativação
        alarmToggle.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                startAlarm()
            } else {
                cancelAlarm()
            }
            updateStatusText(isChecked)
        }
    }

    private fun startAlarm() {
        // Usa setInexactRepeating para agendamento de alarmes a cada intervalo.
        // RTC_WAKEUP garante que o dispositivo acorde para disparar o alarme mesmo em modo Doze.

        // 1. O PRIMEIRO ALARME DEVE TOCAR APÓS O INTERVALO
        val firstTriggerTime = System.currentTimeMillis() + ALARM_INTERVAL_MS

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstTriggerTime, // Toca a primeira vez após o intervalo
            ALARM_INTERVAL_MS, // E repete a cada intervalo
            pendingIntent
        )

        // Exibe a mensagem de confirmação
        val intervalInSeconds = ALARM_INTERVAL_MS / 1000
        Toast.makeText(this, "Alarme DE BEBER AGUA ATIVADO! Toca a cada $intervalInSeconds segundos.", Toast.LENGTH_LONG).show()
    }

    private fun cancelAlarm() {
        // Cancela o PendingIntent associado
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Alarme DESATIVADO!", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatusText(isActive: Boolean) {
        val intervalInSeconds = ALARM_INTERVAL_MS / 1000
        if (isActive) {
            statusText.text = "Status: Alarme ATIVO (Dispara a cada $intervalInSeconds segundos) 🔔"
            alarmToggle.text = "Desativar Alarme"
        } else {
            statusText.text = "Status: Alarme INATIVO"
            alarmToggle.text = "Ativar Alarme"
        }
    }
}