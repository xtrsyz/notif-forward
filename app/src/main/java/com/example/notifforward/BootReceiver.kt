package com.example.notifforward

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

/**
 * Starts the foreground service automatically after a device reboot so the
 * forwarder resumes without any user interaction (as long as the toggle was
 * left enabled before the reboot).
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        val enabled = Prefs.isEnabled(context)
        Log.d("BootReceiver", "Boot completed – forwarder enabled=$enabled")

        if (enabled) {
            val serviceIntent = ForwarderForegroundService.buildIntent(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
