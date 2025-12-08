package com.exemplo.xiomealarm

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

class MainActivity : AppCompatActivity() {

    private lateinit var alarmManager: AlarmManager
    private lateinit var prefs: SharedPreferences

    private val ALARM_ACTION = "com.exemplo.xiomealarm.ALARM_TRIGGER"

    private val PREFS_NAME = "AlarmPrefs"
    private val KEY_ALARM_ACTIVE = "IsAlarmActive"
    private val KEY_TOTAL_CONSUMED_TODAY = "TotalConsumedToday"
    private val KEY_ALARM_INTERVAL = "AlarmIntervalMs"
    private val KEY_DEFAULT_VOLUME = "DefaultVolumeMl"

    private lateinit var alarmSwitch: Switch
    private lateinit var statusTextView: TextView
    private lateinit var totalConsumedTextView: TextView
    private lateinit var resetButton: Button
    private lateinit var intervalPicker: NumberPicker
    private lateinit var volumePicker: NumberPicker

    private val ALARM_REQUEST_CODE = 0 // Código para o alarme inicial (main activity)
    private val RESCHEDULE_REQUEST_CODE = 1 // Código para o alarme reagendado (AlarmReceiver)


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

        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        alarmSwitch = findViewById(R.id.alarmSwitch)
        statusTextView = findViewById(R.id.statusText)
        totalConsumedTextView = findViewById(R.id.totalConsumedTextView)
        resetButton = findViewById(R.id.resetButton)
        intervalPicker = findViewById(R.id.timePicker)
        volumePicker = findViewById(R.id.volumePicker)

        setupIntervalPicker()
        setupVolumePicker()
        loadSavedState()

        requestExactAlarmPermission()
        requestNotificationPermission()

        alarmSwitch.setOnCheckedChangeListener { _, isChecked -> toggleAlarm(isChecked) }
        resetButton.setOnClickListener { resetConsumption() }

        val filter = IntentFilter("com.exemplo.xiomealarm.ACTION_CONSUMPTION_UPDATED")
        registerReceiver(consumptionUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)


    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                try {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } catch (e: Exception) {
                    Log.e("MainActivity", "Erro ao abrir permissão: ${e.message}")
                }
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Permissão de Notificação é recomendada.", Toast.LENGTH_LONG).show()
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
        val savedIntervalIndex = prefs.getInt(KEY_ALARM_INTERVAL, 0)
        val savedVolumeIndex = prefs.getInt(KEY_DEFAULT_VOLUME, 1)

        intervalPicker.value = savedIntervalIndex
        volumePicker.value = savedVolumeIndex
        alarmSwitch.isChecked = isActive

        updateStatusText(isActive)
        updateTotalConsumedDisplay()
    }

    private fun resetConsumption() {
        prefs.edit().putInt(KEY_TOTAL_CONSUMED_TODAY, 0).apply()
        updateTotalConsumedDisplay()
        Toast.makeText(this, "Contagem diária zerada!", Toast.LENGTH_SHORT).show()
    }

    private fun openFixPermissions() {
        startActivity(Intent(this, FixPermissionsActivity::class.java))
    }

    private fun toggleAlarm(enable: Boolean) {
        if (!enable) {
            cancelAlarm()
            prefs.edit().putBoolean(KEY_ALARM_ACTIVE, false).apply()
            updateStatusText(false)
            return
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            !alarmManager.canScheduleExactAlarms()
        ) {
            Toast.makeText(this, "Permissão de Alarme Exato faltando.", Toast.LENGTH_LONG).show()
            openFixPermissions()
            alarmSwitch.isChecked = false
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, "Permissão de Notificação faltando.", Toast.LENGTH_LONG).show()
            openFixPermissions()
            alarmSwitch.isChecked = false
            return
        }


        // Se tudo ok  ativa o alarme
        val intervalMs = getSelectedIntervalMs()
        val volumeMl = getSelectedVolumeMl()

        setAlarm(intervalMs, volumeMl)
        prefs.edit()
            .putBoolean(KEY_ALARM_ACTIVE, true)
            .putInt(KEY_ALARM_INTERVAL, intervalPicker.value)
            .putInt(KEY_DEFAULT_VOLUME, volumePicker.value)
            .apply()

        updateStatusText(true)
    }

    private fun setAlarm(intervalMs: Long, volumeMl: Int) {
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra("EXTRA_VOLUME_ML", volumeMl)
            putExtra("EXTRA_INTERVAL_MS", intervalMs)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE, // Código 0
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + intervalMs,
            pendingIntent
        )

        Toast.makeText(this, "Alarme ativado!", Toast.LENGTH_LONG).show()
    }

    private fun cancelAlarm() {
        val intent = Intent(this, AlarmReceiver::class.java).apply { action = ALARM_ACTION }

        // 1. Cancela o alarme inicial agendado pelo MainActivity (RequestCode 0)
        val originalPendingIntent = PendingIntent.getBroadcast(
            this,
            ALARM_REQUEST_CODE, // 0
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(originalPendingIntent)

        // 2. Cancela o alarme reagendado pelo AlarmReceiver (RequestCode 1)
        val rescheduledPendingIntent = PendingIntent.getBroadcast(
            this,
            RESCHEDULE_REQUEST_CODE, // 1
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(rescheduledPendingIntent)

        Toast.makeText(this, "Alarme desativado.", Toast.LENGTH_SHORT).show()
    }

    private fun getSelectedIntervalMs(): Long {
        return when (intervalPicker.value) {
            0 -> 3600000L
            1 -> 5400000L
            2 -> 7200000L
            3 -> 9000000L
            4 -> 10800000L
            else -> 7200000L
        }
    }

    private fun getSelectedVolumeMl(): Int {
        return when (volumePicker.value) {
            0 -> 100
            1 -> 200
            2 -> 300
            3 -> 400
            4 -> 500
            else -> 300
        }
    }

    private fun updateStatusText(isActive: Boolean) {
        val label = intervalPicker.displayedValues[intervalPicker.value]
        statusTextView.text = if (isActive) {
            "Status: Alarme ATIVO (a cada $label)"
        } else {
            "Status: Alarme Inativo"
        }
    }

    private fun updateTotalConsumedDisplay() {
        val total = prefs.getInt(KEY_TOTAL_CONSUMED_TODAY, 0)
        totalConsumedTextView.text = "Total Consumido Hoje: $total ml"
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(consumptionUpdateReceiver)
    }
}