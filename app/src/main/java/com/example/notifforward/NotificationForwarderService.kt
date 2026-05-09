package com.example.notifforward

import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Captures notifications posted by [TARGET_PACKAGE] and forwards them to the
 * placeholder API endpoint via [ApiClient].
 *
 * Background reliability notes
 * ----------------------------
 * • [onListenerConnected] – logged for diagnostics.
 * • [onListenerDisconnected] – calls [requestRebind] so the system reconnects
 *   this listener after a process death or crash without requiring a reboot.
 * • [ForwarderForegroundService] keeps the hosting process alive, reducing how
 *   often the system kills it in the first place.
 */
class NotificationForwarderService : NotificationListenerService() {

    companion object {
        private const val TAG = "NotifForwarder"
        const val TARGET_PACKAGE = "com.gojek.gopaymerchant"
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification listener connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Notification listener disconnected – requesting rebind")
        // Ask the system to reconnect this listener without requiring a reboot.
        requestRebind(ComponentName(this, NotificationForwarderService::class.java))
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!Prefs.isEnabled(this)) return
        if (sbn.packageName != TARGET_PACKAGE) return

        val extras = sbn.notification.extras
        val title = extras.getCharSequence("android.title")?.toString()
        val text  = extras.getCharSequence("android.text")?.toString()

        Log.d(TAG, "Captured: $title | $text")
        ApiClient.send(sbn.packageName, title, text, sbn.postTime)
    }
}
