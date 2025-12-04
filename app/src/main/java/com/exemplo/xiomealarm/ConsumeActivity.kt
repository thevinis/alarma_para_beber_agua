package com.exemplo.xiomealarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConsumeActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val PREFS_NAME = "AlarmPrefs"
    private val KEY_TOTAL_CONSUMED_TODAY = "TotalConsumedToday"
    private val NOTIFICATION_ID = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // O tema de diálogo foi removido do Manifest, mas o layout ainda pode parecer um diálogo
        setContentView(R.layout.activity_consume)

        // Inicializa SharedPreferences
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Obtém o volume padrão passado pela notificação
        val volumeMl = intent.getIntExtra("EXTRA_VOLUME_ML", 300)

        // Inicializa Views
        val volumeTextView: TextView = findViewById(R.id.volumeTextView)
        val confirmButton: Button = findViewById(R.id.confirmButton)
        val skipButton: Button = findViewById(R.id.skipButton)

        // Atualiza o texto com o volume
        volumeTextView.text = "Volume Padrão: $volumeMl ml"

        // Listener do Botão Confirmar
        confirmButton.setOnClickListener {
            registerConsumption(volumeMl)
            // Fecha a notificação e a Activity
            clearNotification()
            finish()
        }

        // Listener do Botão Pular
        skipButton.setOnClickListener {
            Toast.makeText(this, "Lembrete ignorado. Tente beber na próxima vez!", Toast.LENGTH_SHORT).show()
            // Apenas fecha a notificação e a Activity
            clearNotification()
            finish()
        }
    }

    private fun registerConsumption(volume: Int) {
        val currentTotal = prefs.getInt(KEY_TOTAL_CONSUMED_TODAY, 0)
        val newTotal = currentTotal + volume

        // Salva o novo total
        prefs.edit().putInt(KEY_TOTAL_CONSUMED_TODAY, newTotal).apply()

        Toast.makeText(this, "$volume ml registrados! Total hoje: $newTotal ml", Toast.LENGTH_SHORT).show()

        // Envia um broadcast para a MainActivity para atualizar o total na interface
        val updateIntent = Intent("com.exemplo.xiomealarm.ACTION_CONSUMPTION_UPDATED")
        sendBroadcast(updateIntent)
    }

    private fun clearNotification() {
        // Cancela a notificação, removendo-a da barra de status
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(NOTIFICATION_ID)
    }
}