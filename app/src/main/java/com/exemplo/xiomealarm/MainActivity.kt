package com.exemplo.xiomealarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch // Adicionado o import do Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : AppCompatActivity() {

    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(this, "Permissão de notificação negada", Toast.LENGTH_SHORT).show()
            }
        }


    private lateinit var alarmManager: AlarmManager
    private lateinit var prefs: SharedPreferences

    // CONSTANTE PARA O ACTION EXPLÍCITO DO RECEIVER
    private val ALARM_ACTION = "com.exemplo.xiomealarm.ALARM_TRIGGER"

    // Variáveis de SharedPreferences
    private val PREFS_NAME = "AlarmPrefs"
    private val KEY_ALARM_ACTIVE = "IsAlarmActive"
    private val KEY_TOTAL_CONSUMED_TODAY = "TotalConsumedToday"
    private val KEY_ALARM_INTERVAL = "AlarmIntervalMs"
    private val KEY_DEFAULT_VOLUME = "DefaultVolumeMl"

    // Variáveis de UI
    private lateinit var alarmSwitch: Switch // ID alterado de toggleButton
    private lateinit var statusTextView: TextView // Novo ID para o status do alarme
    private lateinit var totalConsumedTextView: TextView
    private lateinit var resetButton: Button
    private lateinit var intervalPicker: NumberPicker // Será referenciado como timePicker no layout
    private lateinit var volumePicker: NumberPicker

    // Constantes
    private val ALARM_REQUEST_CODE = 0 // Código do PendingIntent

    // --- RECEIVER PARA ATUALIZAR CONSUMO EM TEMPO REAL ---
    private val consumptionUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "com.exemplo.xiomealarm.ACTION_CONSUMPTION_UPDATED") {
                updateTotalConsumedDisplay()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicialização de serviços e preferências
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Inicialização de Views (IDs ajustados para o novo layout)
        alarmSwitch = findViewById(R.id.alarmSwitch) // Referencia o Switch
        statusTextView = findViewById(R.id.statusText) // Referencia o status
        totalConsumedTextView = findViewById(R.id.totalConsumedTextView)
        resetButton = findViewById(R.id.resetButton)
        intervalPicker = findViewById(R.id.timePicker) // ID mudou para timePicker
        volumePicker = findViewById(R.id.volumePicker)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }


        // Configuração inicial do UI
        setupIntervalPicker()
        setupVolumePicker()
        loadSavedState()

        // --- PONTO CRÍTICO 1: SOLICITAÇÃO DE PERMISSÃO DE ALARME EXATO ---
        requestExactAlarmPermission()

        // --- PONTO CRÍTICO 4: SOLICITAÇÃO DE PERMISSÃO DE NOTIFICAÇÃO (Android 13+) ---
        requestNotificationPermission()

        // Listeners de UI
        // O alarme é ativado/desativado quando o estado do Switch muda
        alarmSwitch.setOnCheckedChangeListener { _, isChecked -> toggleAlarm(isChecked) }
        resetButton.setOnClickListener { resetConsumption() }

        // Registrar o BroadcastReceiver para receber atualizações da ConsumeActivity
        val filter = IntentFilter("com.exemplo.xiomealarm.ACTION_CONSUMPTION_UPDATED")
        registerReceiver(consumptionUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    // --- FUNÇÃO CRÍTICA PARA SOLICITAR PERMISSÃO DE ALARME EXATO ---
    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                try {
                    startActivity(intent)
                    Toast.makeText(this, "Permissão de Alarme Exato solicitada. Por favor, conceda.", Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao abrir tela de permissão de Alarme Exato: ${e.message}")
                    Toast.makeText(this, "Não foi possível abrir a tela de permissão. Conceda manualmente em Configurações.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // --- FUNÇÃO CRÍTICA PARA SOLICITAR PERMISSÃO DE NOTIFICAÇÃO (Android 13+) ---
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (Tiramisu)
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // Lançador de permissão para POST_NOTIFICATIONS
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Permissão de Notificação concedida.", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão de Notificação é recomendada para alertas de água.", Toast.LENGTH_LONG).show()
            }
        }

    private fun setupIntervalPicker() {
        val intervalLabels = arrayOf("1h", "1h 30m", "2h", "2h 30m", "3h")
        intervalPicker.minValue = 0
        intervalPicker.maxValue = intervalLabels.size - 1
        intervalPicker.displayedValues = intervalLabels
    }

    private fun setupVolumePicker() {
        val volumeLabels = arrayOf("100 ml", "200 ml", "300 ml", "400 ml", "500 ml")
        volumePicker.minValue = 0
        volumePicker.maxValue = volumeLabels.size - 1
        volumePicker.displayedValues = volumeLabels
    }

    private fun loadSavedState() {
        val isActive = prefs.getBoolean(KEY_ALARM_ACTIVE, false)
        val savedIntervalIndex = prefs.getInt(KEY_ALARM_INTERVAL, 0) // Padrão 1h
        val savedVolumeIndex = prefs.getInt(KEY_DEFAULT_VOLUME, 1) // Padrão 200 ml

        intervalPicker.value = savedIntervalIndex
        volumePicker.value = savedVolumeIndex
        alarmSwitch.isChecked = isActive // Define o estado inicial do Switch

        updateStatusText(isActive)
        updateTotalConsumedDisplay()
    }

    private fun resetConsumption() {
        prefs.edit().putInt(KEY_TOTAL_CONSUMED_TODAY, 0).apply()
        updateTotalConsumedDisplay()
        Toast.makeText(this, "Contagem diária zerada!", Toast.LENGTH_SHORT).show()
    }

    // Função ajustada para receber o estado do Switch
    private fun toggleAlarm(enable: Boolean) {
        if (!enable) {
            cancelAlarm()
            prefs.edit().putBoolean(KEY_ALARM_ACTIVE, false).apply()
        } else {
            // Verifica permissão crítica antes de tentar agendar
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(this, "Permissão de Alarme Exato NÃO concedida. Conceda e tente novamente.", Toast.LENGTH_LONG).show()
                requestExactAlarmPermission() // Tenta solicitar novamente
                alarmSwitch.isChecked = false // Desativa o switch se a permissão falhar
                return
            }

            // Verifica permissão de Notificação crítica antes de tentar agendar no Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Permissão de Notificação NÃO concedida. Conceda e tente novamente.", Toast.LENGTH_LONG).show()
                requestNotificationPermission() // Tenta solicitar novamente
                alarmSwitch.isChecked = false // Desativa o switch se a permissão falhar
                return
            }

            val intervalMs = getSelectedIntervalMs()
            val volumeMl = getSelectedVolumeMl()

            setAlarm(intervalMs, volumeMl)
            prefs.edit()
                .putBoolean(KEY_ALARM_ACTIVE, true)
                .putInt(KEY_ALARM_INTERVAL, intervalPicker.value)
                .putInt(KEY_DEFAULT_VOLUME, volumePicker.value)
                .apply()
        }
        updateStatusText(enable)
    }

    private fun setAlarm(intervalMs: Long, volumeMl: Int) {
        // CORREÇÃO: Usando o action explícito para o PendingIntent
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION // Define o action que o Manifest espera
            putExtra("EXTRA_DEFAULT_VOLUME_ML", volumeMl)
            putExtra("EXTRA_INTERVAL_MS", intervalMs)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Usa setExactAndAllowWhileIdle (PONTO CRÍTICO DE PRECISÃO)
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 60_000L,
            pendingIntent
        )

        Toast.makeText(this, "Alarme ativado! Próximo em ${intervalMs / 3600000}h.", Toast.LENGTH_LONG).show()
        Log.d("MainActivity", "Alarme agendado: ${intervalMs}ms")
    }

    private fun cancelAlarm() {
        // CORREÇÃO: Usando o action explícito para CANCELAR o PendingIntent corretamente
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION // Define o action para encontrar o PendingIntent existente
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Toast.makeText(this, "Alarme desativado.", Toast.LENGTH_SHORT).show()
    }

    private fun getSelectedIntervalMs(): Long {
        return when (intervalPicker.value) {
            0 -> 3600000L // 1h
            1 -> 5400000L // 1.5h
            2 -> 7200000L // 2h (Padrão)
            3 -> 9000000L // 2.5h
            4 -> 10800000L // 3h
            else -> 7200000L
        }
    }

    private fun getSelectedVolumeMl(): Int {
        return when (volumePicker.value) {
            0 -> 100
            1 -> 200
            2 -> 300 // Padrão
            3 -> 400
            4 -> 500
            else -> 300
        }
    }

    // Função atualizada para usar statusTextView
    private fun updateStatusText(isActive: Boolean) {
        val intervalLabel = intervalPicker.displayedValues[intervalPicker.value]
        if (isActive) {
            statusTextView.text = "Status: Alarme ATIVO (A cada $intervalLabel) 💧"
            // Opcional: Mudar a cor do background do statusText se necessário
        } else {
            statusTextView.text = "Status: Alarme Inativo"
            // Opcional: Mudar a cor do background do statusText se necessário
        }
    }

    private fun updateTotalConsumedDisplay() {
        val total = prefs.getInt(KEY_TOTAL_CONSUMED_TODAY, 0)
        totalConsumedTextView.text = "Total Consumido Hoje: $total ml"
    }

    override fun onDestroy() {
        super.onDestroy()
        // Desregistrar o BroadcastReceiver
        unregisterReceiver(consumptionUpdateReceiver)
    }
}