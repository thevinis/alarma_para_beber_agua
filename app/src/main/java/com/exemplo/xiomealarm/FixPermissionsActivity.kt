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
import android.os.PowerManager
import android.content.Context


class FixPermissionsActivity : AppCompatActivity() {

    private lateinit var btnNotificacao: Button
    private lateinit var btnAlarmeExato: Button
    private lateinit var btnBackground: Button
    private lateinit var btnXiaomi: Button

    private lateinit var btnAutostart: Button



    private fun checkOverlayPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            if (!Settings.canDrawOverlays(this)) {

                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )

                startActivity(intent)
            }
        }
    }

    private fun isBackgroundAllowed(): Boolean {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager

            return pm.isIgnoringBatteryOptimizations(packageName)
        }

        return true
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        startActivity(intent)
    }


    private fun openXiaomiPopupPermission() {
        try {
            val intent = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.permissions.PermissionsEditorActivity"
                )
                putExtra("extra_pkgname", packageName)
            }
            startActivity(intent)
        } catch (e: Exception) {
            startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:$packageName")
                }
            )
        }
    }

    private fun openXiaomiAutostart() {
        try {
            val intent = Intent("miui.intent.action.OP_AUTO_START")
            intent.addCategory(Intent.CATEGORY_DEFAULT)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No Xiaomi, ative também:\nAutoStart",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    // isso detecta autostart apenas nos Xiaomi mais antigos
    private fun isXiaomiAutostartEnabled(): Boolean {
        return try {
            val pm = packageManager
            pm.getPackageInfo("com.miui.securitycenter", 0)
            true
        } catch (e: Exception) {
            false
        }
    }


    private fun refreshPermissionStatus() {

        // NOTIFICAÇÕES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            btnNotificacao.text = if (granted)
                "✔ Permitir Notificações"
            else
                "✘ Permitir Notificações"
        }

        // ALARME EXATO
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(AlarmManager::class.java)
            val granted = alarmManager.canScheduleExactAlarms()

            btnAlarmeExato.text = if (granted)
                "✔ Permitir Alarmes Exatos"
            else
                "✘ Permitir Alarmes Exatos"
        }

        //  BACKGROUND
        val backgroundOk = isBackgroundAllowed()


        btnBackground.text = if (backgroundOk)
            "✔ Executar em Segundo Plano"
        else
            "✘ Executar em Segundo Plano"

        // XIAOMI AUTOSTART
        val xiaomiOk = isXiaomiAutostartEnabled()

        btnXiaomi.text = if (xiaomiOk)
            "✔ Configurar no Xiaomi"
        else
            "✘ Configurar no Xiaomi"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_fix_permissions)

        checkOverlayPermission()

        // Inicializar botões
        btnNotificacao = findViewById(R.id.btnPermNotificacoes)
        btnAlarmeExato = findViewById(R.id.btnPermAlarmes)
        btnBackground = findViewById(R.id.btnPermBackground)
        btnXiaomi = findViewById(R.id.btnPermXiaomi)
        btnAutostart = findViewById(R.id.btnAutostart)

        // NOTIFICAÇÕES
        btnNotificacao.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                if (ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(this, "Permissão já concedida", Toast.LENGTH_SHORT).show()
                } else {
                    requestNotificationPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }

        // ALARMES EXATOS
        btnAlarmeExato.setOnClickListener {

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {

                val alarmManager = getSystemService(AlarmManager::class.java)

                if (!alarmManager.canScheduleExactAlarms()) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                }
            }
        }

        // BACKGROUND
        btnBackground.setOnClickListener {

            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }

        // XIAOMI PERMISSÕES
        btnXiaomi.setOnClickListener {

            openXiaomiPopupPermission()
        }

        // AUTOSTART
        btnAutostart.setOnClickListener {

            openXiaomiAutostart()
        }

        refreshPermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
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
