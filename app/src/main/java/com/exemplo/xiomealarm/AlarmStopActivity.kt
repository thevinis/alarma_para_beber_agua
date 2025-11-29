package com.exemplo.xiomealarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class AlarmStopActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // --- CÓDIGO PARA MOSTRAR ACIMA DA TELA DE BLOQUEIO E ACENDER O DISPLAY ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
        // --------------------------------------------------------------------------

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_stop)

        val stopButton: Button = findViewById(R.id.stopAlarmButton)

        // Adiciona a ação ao botão de parada
        stopButton.setOnClickListener {
            // Chama a função estática no AlarmReceiver, que por sua vez para o Service
            // Usar 'applicationContext' é mais seguro para serviços
            AlarmReceiver.stopAlarm(applicationContext)
            Toast.makeText(this, "Alarme parado!", Toast.LENGTH_SHORT).show()

            // Fecha a tela de parada
            finish()
        }
    }
}