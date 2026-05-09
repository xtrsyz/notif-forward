package com.example.notifforward

import android.util.Log
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Sends notification payloads to the configured endpoint.
 *
 * Replace [ENDPOINT] with your real backend URL before going to production.
 * Requests are executed on a background thread so the caller never blocks.
 */
object ApiClient {

    private const val TAG = "ApiClient"

    /** Placeholder URL – replace with your actual backend endpoint. */
    private const val ENDPOINT = "https://example.com/api/notify"

    fun send(
        packageName: String,
        title: String?,
        text: String?,
        postTime: Long,
    ) {
        thread {
            try {
                val conn = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                val body = JSONObject().apply {
                    put("packageName", packageName)
                    put("title", title ?: "")
                    put("text", text ?: "")
                    put("postTime", postTime)
                }.toString().toByteArray(Charsets.UTF_8)

                conn.outputStream.use { it.write(body) }
                Log.d(TAG, "Forwarded notification – HTTP ${conn.responseCode}")
                conn.disconnect()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to forward notification", e)
            }
        }
    }
}
