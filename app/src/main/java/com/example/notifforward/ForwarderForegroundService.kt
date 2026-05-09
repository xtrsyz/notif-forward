package com.example.notifforward

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.util.Log
import androidx.core.app.NotificationCompat

/**
 * Lightweight foreground service whose sole purpose is to keep the app process
 * alive so that [NotificationForwarderService] is less likely to be killed by
 * the OS when the device is under memory pressure.
 *
 * • Shows a low-importance persistent notification while active.
 * • Returns [START_STICKY] so Android automatically restarts it after a kill.
 * • On start, calls [NotificationListenerService.requestRebind] to reconnect
 *   the notification listener if it became detached (e.g. after a reboot).
 */
class ForwarderForegroundService : Service() {

    companion object {
        private const val TAG = "ForwarderFgService"
        private const val CHANNEL_ID = "forwarder_channel"
        private const val NOTIFICATION_ID = 1001

        /** Convenience factory so callers don't need to import this class directly. */
        fun buildIntent(context: Context) =
            Intent(context, ForwarderForegroundService::class.java)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Foreground service started")
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        // Reconnect the notification listener if the system lost the binding
        // (e.g. after a process restart or device reboot).
        NotificationListenerService.requestRebind(
            ComponentName(this, NotificationForwarderService::class.java)
        )

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Foreground service stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    // -------------------------------------------------------------------------

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.channel_description)
            }
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(channel)
        }
    }

    private fun buildForegroundNotification(): Notification {
        val tapIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.service_running))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(tapIntent)
            .setOngoing(true)
            .build()
    }
}
