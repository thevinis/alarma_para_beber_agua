package com.exemplo.xiomealarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ConsumeActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private val PREFS_NAME = "AlarmPrefs"
    private val KEY_TOTAL_CONSUMED_TODAY = "TotalConsumedToday"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_consume)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val volumeMl = intent.getIntExtra(AlarmService.EXTRA_VOLUME_ML, 300)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }


        val volumeTextView: TextView = findViewById(R.id.volumeTextView)
        val confirmButton: Button = findViewById(R.id.confirmButton)
        val skipButton: Button = findViewById(R.id.skipButton)

        volumeTextView.text = "Volume Padrão: $volumeMl ml"

        confirmButton.setOnClickListener {
            registerConsumption(volumeMl)
            stopAlarm()
            clearNotification()
            finish()
        }

        skipButton.setOnClickListener {
            stopAlarm()
            clearNotification()
            finish()
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun stopAlarm() {
        val stopIntent = Intent(this, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP
        }
        startService(stopIntent)
    }

    private fun registerConsumption(volume: Int) {
        val current = prefs.getInt(KEY_TOTAL_CONSUMED_TODAY, 0)
        val newTotal = current + volume

        prefs.edit().putInt(KEY_TOTAL_CONSUMED_TODAY, newTotal).apply()

        Toast.makeText(this, "$volume ml registrados! Total hoje: $newTotal ml", Toast.LENGTH_SHORT).show()

        sendBroadcast(Intent("com.exemplo.xiomealarm.ACTION_CONSUMPTION_UPDATED"))
    }

    private fun clearNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(AlarmService.NOTIFICATION_ID)
    }
}
