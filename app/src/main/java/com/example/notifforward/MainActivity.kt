package com.example.notifforward

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Button
import android.widget.CompoundButton
import android.widget.Switch
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

/**
 * Single-screen UI that lets the user:
 *
 * 1. Toggle forwarding on/off ([Switch]).
 * 2. Open the system Notification Access settings to grant listener permission.
 * 3. Request battery-optimization exemption so Android is less likely to kill
 *    the foreground service.
 *
 * Status warnings are shown inline when either prerequisite is missing.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var enableSwitch: Switch
    private lateinit var btnListenerSettings: Button
    private lateinit var btnBatteryOpt: Button

    /** Requests POST_NOTIFICATIONS permission (Android 13+). Non-blocking result. */
    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Result is informational only; foreground service starts regardless.
        }

    /**
     * Named listener so [updateUi] can detach it before programmatically
     * updating the switch state, preventing an unwanted recursive call.
     */
    private val switchListener = CompoundButton.OnCheckedChangeListener { _, isChecked ->
        Prefs.setEnabled(this, isChecked)
        if (isChecked) startForwarder() else stopForwarder()
        updateUi()
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusText          = findViewById(R.id.statusText)
        enableSwitch        = findViewById(R.id.enableSwitch)
        btnListenerSettings = findViewById(R.id.btnListenerSettings)
        btnBatteryOpt       = findViewById(R.id.btnBatteryOpt)

        enableSwitch.setOnCheckedChangeListener(switchListener)

        btnListenerSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        btnBatteryOpt.setOnClickListener {
            requestBatteryExemption()
        }
    }

    override fun onResume() {
        super.onResume()
        maybeRequestPostNotificationPermission()
        // Re-ensure the foreground service is running if the toggle is on.
        if (Prefs.isEnabled(this)) startForwarder()
        updateUi()
    }

    // -------------------------------------------------------------------------
    // Foreground service helpers
    // -------------------------------------------------------------------------

    private fun startForwarder() {
        val intent = ForwarderForegroundService.buildIntent(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopForwarder() {
        stopService(ForwarderForegroundService.buildIntent(this))
    }

    // -------------------------------------------------------------------------
    // UI helpers
    // -------------------------------------------------------------------------

    private fun updateUi() {
        val enabled    = Prefs.isEnabled(this)
        val listenerOk = isNotificationListenerEnabled()
        val batteryOk  = isBatteryOptimizationIgnored()

        // Detach the listener before setting isChecked programmatically so it
        // doesn't trigger the user-interaction path again.
        enableSwitch.setOnCheckedChangeListener(null)
        enableSwitch.isChecked = enabled
        enableSwitch.setOnCheckedChangeListener(switchListener)

        statusText.text = buildString {
            append(
                if (enabled) getString(R.string.status_active)
                else getString(R.string.status_inactive)
            )
            if (!listenerOk) append("\n⚠ ${getString(R.string.warning_listener)}")
            if (!batteryOk)  append("\n⚠ ${getString(R.string.warning_battery)}")
        }

        btnBatteryOpt.text = if (batteryOk)
            getString(R.string.battery_opt_ok)
        else
            getString(R.string.battery_opt_request)
    }

    // -------------------------------------------------------------------------
    // Permission / system-setting checks
    // -------------------------------------------------------------------------

    private fun isNotificationListenerEnabled(): Boolean {
        val flat = Settings.Secure.getString(
            contentResolver, "enabled_notification_listeners"
        ) ?: return false
        val target = ComponentName(this, NotificationForwarderService::class.java)
        return flat.split(":").any {
            ComponentName.unflattenFromString(it) == target
        }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(packageName)
    }

    private fun requestBatteryExemption() {
        if (isBatteryOptimizationIgnored()) return
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    private fun maybeRequestPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
