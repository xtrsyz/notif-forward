# notif-forward

Android **notification forwarder** – a minimal Kotlin starter project that captures notifications from `com.gojek.gopaymerchant` and forwards them to a configurable API endpoint.

## Features

| Feature | Details |
|---|---|
| Notification capture | `NotificationListenerService` – only processes notifications from `com.gojek.gopaymerchant` |
| API forwarding | HTTP POST to a placeholder endpoint (`https://example.com/api/notify`) – replace with your real URL in `ApiClient.kt` |
| Enable / disable toggle | `SharedPreferences`-backed switch in `MainActivity` |
| Boot auto-start | `BootReceiver` starts the foreground service on `BOOT_COMPLETED` |
| Background survivability | `ForwarderForegroundService` (foreground service, `START_STICKY`) keeps the process alive and calls `requestRebind` to reconnect the notification listener after a process restart |
| Battery optimisation | `MainActivity` detects whether battery optimisation is active and guides the user to the exemption settings screen |
| Android 13+ | Runtime request for `POST_NOTIFICATIONS` permission |

## Project structure

```
app/src/main/
├── AndroidManifest.xml
└── java/com/example/notifforward/
    ├── MainActivity.kt                 — UI: toggle, warnings, settings links
    ├── NotificationForwarderService.kt — NotificationListenerService with auto-rebind
    ├── ForwarderForegroundService.kt   — Foreground service (background keepalive)
    ├── BootReceiver.kt                 — Restart forwarder after reboot
    ├── Prefs.kt                        — SharedPreferences helper
    └── ApiClient.kt                    — HTTP POST client (placeholder endpoint)
```

## Quick start

1. Clone the repo and open it in **Android Studio**.
2. Build and install the app (`minSdk 26`, `targetSdk 34`).
3. Open the app and tap **Buka Notification Access** → grant access to *Notification Forwarder*.
4. Tap **Nonaktifkan Battery Optimization** and approve the exemption.
5. Toggle **Aktifkan forwarder** to start forwarding.

## Configuration

Replace the placeholder URL in `ApiClient.kt`:

```kotlin
private const val ENDPOINT = "https://your-real-backend.com/api/notify"
```

The JSON payload sent on each notification:

```json
{
  "packageName": "com.gojek.gopaymerchant",
  "title": "...",
  "text": "...",
  "postTime": 1715000000000
}
```

## Requirements

- Android 8.0+ (API 26)
- Java 17+ for building
- Android Studio Hedgehog or newer (recommended)