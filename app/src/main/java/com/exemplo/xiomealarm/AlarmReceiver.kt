package com.exemplo.xiomealarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.core.content.ContextCompat

// Este componente é o "gatilho". Ele é rápido e apenas inicia o Service.
class AlarmReceiver : BroadcastReceiver() {

    companion object {
        fun stopAlarm(context: Context) {
            AlarmService.stopService(context)
            Log.i("XiomeAlarm", "Comando de parada enviado ao Service.")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.i("XiomeAlarm", "Alarme Disparado! Iniciando AlarmService como Foreground.")
        Toast.makeText(context, "ALARME HORÁRIO: Iniciando Service... ⏰", Toast.LENGTH_SHORT).show()

        // 1. Cria o Intent para iniciar o Service
        val serviceIntent = Intent(context, AlarmService::class.java)

        // 2. INICIA O SERVICE COMO FOREGROUND. Isso é crucial no Android 8.0+
        // O ContextCompat.startForegroundService garante que o metodo correto seja chamado
        // dependendo da versão do Android, evitando erros de compatibilidade.
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}