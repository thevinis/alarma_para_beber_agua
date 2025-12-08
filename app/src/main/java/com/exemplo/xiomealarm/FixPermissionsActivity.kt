package com.exemplo.xiomealarm

import android.Manifest
import android.app.AlarmManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class FixPermissionsActivity : AppCompatActivity() {

    private lateinit var btnNotificacao: Button
    private lateinit var btnAlarmeExato: Button
    private lateinit var btnBackground: Button
    private lateinit var btnXiaomi: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fix_permissions)

        btnNotificacao = findViewById(R.id.btnPermNotificacoes)
        btnAlarmeExato = findViewById(R.id.btnPermAlarmes)
        btnBackground = findViewById(R.id.btnPermBackground)
        btnXiaomi = findViewById(R.id.btnPermXiaomi)

        // ------------------------------
        // ✔ PERMISSÃO DE NOTIFICAÇÕES
        // ------------------------------
        btnNotificacao.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Permissão de Notificação já concedida.", Toast.LENGTH_SHORT).show()
                } else {
                    requestNotificationPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                Toast.makeText(this, "Esta versão do Android não precisa dessa permissão.", Toast.LENGTH_SHORT).show()
            }
        }

        // ------------------------------
        // ✔ PERMISSÃO DE ALARMES EXATOS
        // ------------------------------
        btnAlarmeExato.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(AlarmManager::class.java)
                if (!alarmManager.canScheduleExactAlarms()) {
                    try {
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    } catch (e: Exception) {
                        Toast.makeText(
                            this,
                            "Não foi possível abrir a tela de permissão.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    Toast.makeText(this, "Permissão de alarme exato já concedida.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Seu Android não exige essa permissão.", Toast.LENGTH_SHORT).show()
            }
        }

        // ------------------------------
        // ✔ PERMISSÃO DE EXECUTAR EM SEGUNDO PLANO
        // ------------------------------
        btnBackground.setOnClickListener {
            try {
                val intent = Intent().apply {
                    action = Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS
                }
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Abra manualmente: Configurações > Bateria > Sem restrições", Toast.LENGTH_LONG).show()
            }
        }

        // ------------------------------
        // ✔ CONFIGURAÇÕES ESPECIAIS PARA XIAOMI / REDMI / POCO
        // ------------------------------
        btnXiaomi.setOnClickListener {
            try {
                val intent = Intent("miui.intent.action.OP_AUTO_START")
                intent.addCategory(Intent.CATEGORY_DEFAULT)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(
                    this,
                    "No Xiaomi, procure:\nConfigurações > Apps > Permissões > AutoStart",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    // Permissão de notificação
    private val requestNotificationPerm =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                Toast.makeText(this, "Notificações permitidas!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "É recomendado permitir notificações.", Toast.LENGTH_LONG).show()
            }
        }
}
